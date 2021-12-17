package org.folio.des.scheduling;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.scheduling.ExportTaskTrigger;
import org.folio.des.service.JobService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Lazy(false)
@Log4j2
@RequiredArgsConstructor
public class DefaultExportTriggerTaskRegistrar implements DisposableBean {
  String cronsExpressions = "0/3 * * * * *|0/15 * * * * *|0/5 * * * * *|0/8 * * * * *|0/7 * * * * *|0/5 * * * * *|0/5 * * * * *";
  private final Map<ExportTaskTrigger, Pair<ExportTaskTrigger, ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>(20);
  @Getter
  private final FolioExecutionContextHelper contextHelper;
  @Getter
  private final ThreadPoolTaskScheduler taskScheduler;
  private final JobService jobService;
  private final Converter<ExportConfig, List<ExportTaskTrigger>> triggerConverter;


  @PostConstruct
  public void postConstruct() {
    taskScheduler.setPoolSize(10);
    taskScheduler.initialize();
    configureTasks();
  }

  @Override
  public void destroy() {
    if (taskScheduler != null) {
      log.debug("Shutdown configuration scheduler");
      taskScheduler.shutdown();
    }
    log.debug("Clear scheduled tasks");
    this.scheduledTasks.clear();
  }

  public void configureTasks() {
    Stream.of(StringUtils.split(cronsExpressions, "|")).forEach(cronExpression -> {
      Runnable runnable = () -> {
        log.info("Executor parameter : " + taskScheduler.getActiveCount() + " Poll size : " + taskScheduler.getPoolSize());
        log.info(cronExpression + " My Trigger task executed at " + new Date() + " thread : " + Thread.currentThread().getName());
      };
      Trigger trigger = triggerContext -> {
        CronTrigger crontrigger = new CronTrigger(cronExpression);
        return crontrigger.nextExecutionTime(triggerContext);
      };
      ScheduledFuture<?> task = taskScheduler.schedule(runnable, trigger);
      log.info("Task name : " + task.isDone() + " " + task.isCancelled());
    });
  }

  public void scheduleExportJob(ExportConfig exportConfig) {
    if (exportConfig != null) {
      List<ExportTaskTrigger> triggers = triggerConverter.convert(exportConfig);
      if (CollectionUtils.isNotEmpty(triggers)) {
        triggers.forEach(exportTaskTrigger -> {
          Pair<ExportTaskTrigger, ScheduledFuture<?>> triggerWithScheduleTask = scheduledTasks.get(exportTaskTrigger);
          if (triggerWithScheduleTask != null) {
             if (triggerWithScheduleTask.getKey().getScheduleParameters().equals(exportTaskTrigger.getScheduleParameters())) {
               String scheduleId = extractScheduleId(triggerWithScheduleTask);
               log.info("Task for rescheduling was found : " + scheduleId);
               removeTriggerTask(triggerWithScheduleTask);
               scheduleTask(exportConfig, exportTaskTrigger);
             }
          } else {
            scheduleTask(exportConfig, exportTaskTrigger);
          }
        });
      }
    }
  }

  private void scheduleTask(ExportConfig exportConfig, ExportTaskTrigger exportTaskTrigger) {
    Runnable task = buildTask(exportConfig);
    ScheduledFuture<?> newScheduledTask = this.taskScheduler.schedule(task, exportTaskTrigger);
    this.scheduledTasks.put(exportTaskTrigger, ImmutablePair.of(exportTaskTrigger, newScheduledTask));
  }

  private String extractScheduleId(Pair<ExportTaskTrigger, ScheduledFuture<?>> triggerWithScheduleTask) {
    return Optional.ofNullable(triggerWithScheduleTask.getKey())
                   .map(ExportTaskTrigger::getScheduleParameters)
                   .map(ScheduleParameters::getId)
                   .map(UUID::toString)
                   .orElse(triggerWithScheduleTask.toString());
  }

  private void removeTriggerTask(Pair<ExportTaskTrigger, ScheduledFuture<?>> triggerWithScheduleTask) {
    Pair<ExportTaskTrigger, ScheduledFuture<?>> scheduledTask = scheduledTasks.remove(triggerWithScheduleTask.getKey());
    String scheduleId = extractScheduleId(triggerWithScheduleTask);;
    log.info("Trigger removed : " + scheduleId);
    if (scheduledTask != null && scheduledTask.getValue() != null) {
      scheduledTask.getValue().cancel(true);
      log.info("Future task canceled : " + scheduleId);
    }
  }

  protected Runnable buildTask(ExportConfig exportConfig) {
    return () -> {
      var current = new Date();
      log.info("configureTasks attempt to execute at: {}: is module registered: {} ", current, contextHelper.isModuleRegistered());
      Optional<Job> scheduledJob = createScheduledJob(exportConfig);
      if (scheduledJob.isPresent() && contextHelper.isModuleRegistered()) {
        contextHelper.initScope();
        jobService.upsert(scheduledJob.get());
        log.info("configureTasks executed for jobId: {} at: {}", scheduledJob.get().getId(), current);
      }
    };
  }

  private Optional<Job> createScheduledJob(ExportConfig exportConfig) {
    Job scheduledJob;
    if (exportConfig == null) {
      return Optional.empty();
    } else {
      scheduledJob = new Job();
      scheduledJob.setType(exportConfig.getType());
      scheduledJob.setIsSystemSource(true);
      scheduledJob.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
      log.info("Scheduled job assigned {}.", scheduledJob);
      return Optional.of(scheduledJob);
    }
  }
}
