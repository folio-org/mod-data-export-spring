package org.folio.des.scheduling.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.des.builder.scheduling.ScheduledTaskBuilder;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.folio.des.scheduling.ExportJobScheduler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class BaseExportJobScheduler implements DisposableBean, ExportJobScheduler {
  protected final Map<ExportTaskTrigger, Pair<ExportTaskTrigger, ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>(20);
  protected final ThreadPoolTaskScheduler taskScheduler;
  protected final Converter<ExportConfig, List<ExportTaskTrigger>> triggerConverter;
  protected final ScheduledTaskBuilder scheduledTaskBuilder;
  private final int poolSize;

  @PostConstruct
  public void postConstruct() {
    taskScheduler.setPoolSize(poolSize);
    taskScheduler.initialize();
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

  @Override
  public List<Job> scheduleExportJob(ExportConfig exportConfig) {
    List<Job> scheduledJobs = new ArrayList<>();
    if (exportConfig != null) {
      List<ExportTaskTrigger> triggers = triggerConverter.convert(exportConfig);
      if (CollectionUtils.isNotEmpty(triggers)) {
        triggers.forEach(exportTaskTrigger -> {
          Pair<ExportTaskTrigger, ScheduledFuture<?>> triggerWithScheduleTask = scheduledTasks.get(exportTaskTrigger);
          if (triggerWithScheduleTask != null) {
             String scheduleId = extractScheduleId(triggerWithScheduleTask);
             if (!triggerWithScheduleTask.getKey().getScheduleParameters().equals(exportTaskTrigger.getScheduleParameters())) {
               removeTriggerTask(triggerWithScheduleTask);
               scheduleTask(exportConfig, exportTaskTrigger).ifPresent(scheduledJobs::add);
               log.info("Task for rescheduling was found : " + scheduleId);
             }
          } else {
            scheduleTask(exportConfig, exportTaskTrigger).ifPresent(scheduledJobs::add);
            log.info("New Task scheduled");
          }
        });
      }
    }
    return scheduledJobs;
  }

  @Override
  public void initAllScheduledJob() {
    log.info("Init method is not implemented for Based scheduled service");
    return;
  }

  public Map<ExportTaskTrigger, Pair<ExportTaskTrigger, ScheduledFuture<?>>> getScheduledTasks() {
    return Collections.unmodifiableMap(scheduledTasks);
  }

  private Optional<Job> scheduleTask(ExportConfig exportConfig, ExportTaskTrigger exportTaskTrigger) {
    Optional<ScheduledTask> scheduledTask = scheduledTaskBuilder.buildTask(exportConfig);
    if (scheduledTask.isPresent()) {
      ScheduledFuture<?> newScheduledTask = this.taskScheduler.schedule(scheduledTask.get().getTask(), exportTaskTrigger);
      this.scheduledTasks.put(exportTaskTrigger, ImmutablePair.of(exportTaskTrigger, newScheduledTask));

      return Optional.ofNullable(scheduledTask.get().getJob());
    }
    log.info("Scheduled job is not created and wasn't scheduled");
    return Optional.empty();
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
}
