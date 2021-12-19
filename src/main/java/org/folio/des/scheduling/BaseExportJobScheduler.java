package org.folio.des.scheduling;

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
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.scheduling.ExportTaskTrigger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public abstract class BaseExportJobScheduler implements DisposableBean, ExportJobScheduler {
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

  public void scheduleExportJob(ExportConfig exportConfig) {
    if (exportConfig != null) {
      List<ExportTaskTrigger> triggers = triggerConverter.convert(exportConfig);
      if (CollectionUtils.isNotEmpty(triggers)) {
        triggers.forEach(exportTaskTrigger -> {
          Pair<ExportTaskTrigger, ScheduledFuture<?>> triggerWithScheduleTask = scheduledTasks.get(exportTaskTrigger);
          if (triggerWithScheduleTask != null) {
             if (!triggerWithScheduleTask.getKey().getScheduleParameters().equals(exportTaskTrigger.getScheduleParameters())) {
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
    Runnable task = scheduledTaskBuilder.buildTask(exportConfig);
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
}
