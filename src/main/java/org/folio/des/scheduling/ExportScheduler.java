package org.folio.des.scheduling;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.security.AuthtService;
import org.folio.des.service.ExportConfigService;
import org.folio.des.service.JobService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExportScheduler implements SchedulingConfigurer {

  private ScheduledTaskRegistrar registrar;
  private Job scheduledJob;
  private final ExportTrigger trigger;
  private final JobService jobService;
  private final ExportConfigService configService;
  private final AuthtService authtService;

  private void fetchConfiguration() {
    Optional<ExportConfig> savedConfig = configService.getConfig();
    savedConfig.ifPresent(trigger::setConfig);
    savedConfig.ifPresent(exportConfig -> scheduledJob = defaultJob(exportConfig));
  }

  private Executor taskExecutor() {
    return Executors.newScheduledThreadPool(100);
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    registrar = taskRegistrar;
    taskRegistrar.setScheduler(taskExecutor());

    taskRegistrar.addTriggerTask(
        () -> {
          if (authtService.isTenantRegistered()) {
            authtService.initializeFolioScope();
            jobService.upsert(scheduledJob);
          }
        },
        trigger);
  }

  public void initScheduleConfiguration() {
    fetchConfiguration();
    reconfigureSchedule();
  }

  public void updateTasks(ExportConfig exportConfig) {
    trigger.setConfig(exportConfig);
    reconfigureSchedule();
  }

  private void reconfigureSchedule() {
    if (registrar.hasTasks()) {
      registrar.destroy();
      registrar.afterPropertiesSet();
    }
  }

  private Job defaultJob(ExportConfig exportConfig) {
    Job job = new Job();
    job.setType(exportConfig.getType());
    job.setIsSystemSource(true);

    var exportTypeSpecificParameters = exportConfig.getExportTypeSpecificParameters();
    if (exportTypeSpecificParameters == null) {
      log.error("There is no configuration for scheduled job");
      return job;
    }
    job.setExportTypeSpecificParameters(exportTypeSpecificParameters);

    return job;
  }
}
