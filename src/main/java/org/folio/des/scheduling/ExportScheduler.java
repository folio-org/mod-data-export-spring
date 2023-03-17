package org.folio.des.scheduling;

import static java.util.Objects.nonNull;

import java.util.Date;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.JobService;
import org.folio.des.service.config.ExportConfigService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

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
  private final FolioExecutionContext folioExecutionContext;

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
        try (var context = new FolioExecutionContextSetter(contextHelper.getFolioExecutionContext(scheduledJob.getTenant()))) {
          Job resultJob = jobService.upsertAndSendToKafka(scheduledJob, true);
          log.info("configureTasks executed for jobId: {} at: {}", resultJob.getId(), current);
        }
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
    if (contextHelper.isModuleRegistered() && nonNull(scheduledJob.getTenant())) {
      try (var context = new FolioExecutionContextSetter(contextHelper.getFolioExecutionContext(scheduledJob.getTenant()))) {
        jobService.deleteOldJobs();
      }
      log.info("deleteOldJobs executed for jobId: {} at: {}", scheduledJob.getId(), current);
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
      scheduledJob.setTenant(exportConfig.getTenant());
    }
    log.info("Scheduled job assigned {}.", scheduledJob);
  }

  private ExportConfig fetchConfiguration() {
    Optional<ExportConfig> savedConfig = burSarExportConfigService.getFirstConfig();
    if (savedConfig.isPresent()) {
      log.info("Got {}.", savedConfig.get());
      var exportConfig = savedConfig.get();
      exportConfig.setTenant(folioExecutionContext.getTenantId());
      return exportConfig;
    } else {
      log.info("No export schedules found.");
      return null;
    }
  }

}
