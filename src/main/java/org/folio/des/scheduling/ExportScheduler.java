package org.folio.des.scheduling;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Date;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.JobService;
import org.folio.des.service.config.ExportConfigService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Lazy(false)
@Component
@Configuration
@EnableScheduling
@Log4j2
@RequiredArgsConstructor
public class ExportScheduler implements SchedulingConfigurer {

  private final ExportTrigger trigger;
  private final JobService jobService;
  private final ExportConfigService burSarExportConfigService;
  private final FolioExecutionContextHelper contextHelper;

  private final Queue<ExportConfig> exportConfigQueue = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean nextJobAllowed = new AtomicBoolean(true);

  private ScheduledTaskRegistrar registrar;
  private Job scheduledJob;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    registrar = taskRegistrar;
    taskRegistrar.setScheduler(Executors.newScheduledThreadPool(100));
    taskRegistrar.addTriggerTask(() -> {
      var current = new Date();
      log.info("configureTasks attempt to execute at: {}: is module registered: {} ", current, contextHelper.isModuleRegistered());
      if (contextHelper.isModuleRegistered()) {
        contextHelper.initScope();
        Job resultJob = jobService.upsert(scheduledJob);
        log.info("configureTasks executed for jobId: {} at: {}", resultJob.getId(), current);
      }

    }, trigger);
  }

  public void initScheduleConfiguration() {
    updateTasks(fetchConfiguration());
  }

  public void updateTasks(ExportConfig exportConfig) {
    log.info("updateTasks queue size {}", exportConfigQueue.size());
    if (nonNull(exportConfig)) {
      exportConfigQueue.add(exportConfig);
      if (nextJobAllowed.get()) {
        nextJob();
      }
    }
  }

  public void nextJob() {
    if (!exportConfigQueue.isEmpty()) {
      log.info("nextJob current task id {}, scheduled period {}, scheduled time {}",
        exportConfigQueue.peek().getSchedulePeriod(), exportConfigQueue.peek().getSchedulePeriod(), exportConfigQueue.peek().getScheduleTime());
      trigger.setConfig(exportConfigQueue.peek());
      createScheduledJob(exportConfigQueue.poll());
      reconfigureSchedule();
      nextJobAllowed.set(false);
    } else {
      nextJobAllowed.set(true);
    }
  }

  @Scheduled(fixedRateString = "P1D")
  public void deleteOldJobs() {
    var current = new Date();
    log.info("deleteOldJobs attempt to execute at: {}: is module registered: {} ", current, contextHelper.isModuleRegistered());
    if (contextHelper.isModuleRegistered()) {
      contextHelper.initScope();
      jobService.deleteOldJobs();
      log.info("deleteOldJobs executed for jobId: {} at: {}", isNull(scheduledJob) ? EMPTY : scheduledJob.getId(), current);

    }
  }

  private void reconfigureSchedule() {
    if (registrar.hasTasks()) {
      registrar.destroy();
      registrar.afterPropertiesSet();
    }
  }

  private void createScheduledJob(ExportConfig exportConfig) {
    if (exportConfig == null) {
      scheduledJob = null;
    } else {
      scheduledJob = new Job();
      scheduledJob.setType(exportConfig.getType());
      scheduledJob.setIsSystemSource(true);
      scheduledJob.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
    }
    log.info("Scheduled job assigned {}.", scheduledJob);
  }

  private ExportConfig fetchConfiguration() {
    Optional<ExportConfig> savedConfig = burSarExportConfigService.getFirstConfig();
    if (savedConfig.isPresent()) {
      log.info("Got {}.", savedConfig.get());
      return savedConfig.get();
    } else {
      log.info("No export schedules found.");
      return null;
    }
  }

}
