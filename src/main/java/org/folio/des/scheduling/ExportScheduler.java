package org.folio.des.scheduling;

import java.util.Optional;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.ExportConfigService;
import org.folio.des.service.JobService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Log4j2
@RequiredArgsConstructor
public class ExportScheduler implements SchedulingConfigurer {

  private final ExportTrigger trigger;
  private final JobService jobService;
  private final ExportConfigService configService;
  private final FolioExecutionContextHelper contextHelper;

  private ScheduledTaskRegistrar registrar;
  private Job scheduledJob;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    registrar = taskRegistrar;
    taskRegistrar.setScheduler(Executors.newScheduledThreadPool(100));
    taskRegistrar.addTriggerTask(() -> {
      contextHelper.initScope();
      jobService.upsert(scheduledJob);
    }, trigger);
  }

  public void initScheduleConfiguration() {
    updateTasks(fetchConfiguration());
  }

  public void updateTasks(ExportConfig exportConfig) {
    trigger.setConfig(exportConfig);
    createScheduledJob(exportConfig);
    reconfigureSchedule();
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
    Optional<ExportConfig> savedConfig = configService.getConfig();
    if (savedConfig.isPresent()) {
      log.info("Got {}.", savedConfig.get());
      return savedConfig.get();
    } else {
      log.info("No export schedules found.");
      return null;
    }
  }

}
