package org.folio.des.scheduling.quartz.job.acquisition;

import static org.folio.des.scheduling.quartz.QuartzConstants.EXPORT_CONFIG_ID_PARAM;
import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import org.folio.des.builder.job.JobCommandSchedulerBuilder;
import org.folio.des.client.DataExportSpringClient;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.exceptions.SchedulingException;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.service.SystemUserProperties;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class EdifactJob implements org.quartz.Job {
  private static final String PARAM_NOT_FOUND_MESSAGE = "'%s' param is missing in the jobExecutionContext";
  private final ExportTypeBasedConfigManager exportTypeBasedConfigManager;
  private final JobService jobService;
  private final JobCommandSchedulerBuilder jobCommandSchedulerBuilder;
  private final JobExecutionService jobExecutionService;
  private final SystemUserScopedExecutionService executionService;
  private final SystemUserProperties systemUserProperties;
  private final DataExportSpringClient dataExportSpringClient;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    try {
      String tenantId = getTenantId(jobExecutionContext);
      executionService.executeSystemUserScoped(tenantId, () -> {
        Job job = getJob(jobExecutionContext);
        Job resultJob = jobService.upsertAndSendToKafka(job, false, false);
        log.info("execute:: configured task saved in DB jobId: {}", resultJob.getId());
        if (resultJob.getId() != null) {
          // To support RTR the job is calling job execution service via module's API /jobs/send.
          // This doesn't work for Eureka. Solution is to call the service directly in case of system user functionality
          // is disabled (Eureka mode) and call the API in another case
          if (systemUserProperties.isEnabled()) {
            dataExportSpringClient.sendJob(resultJob);
          } else {
            jobExecutionService.sendJobCommand(jobCommandSchedulerBuilder.buildJobCommand(resultJob));
          }
          log.info("execute:: configured task scheduled and sent to kafka for jobId: {}", resultJob.getId());
        }
        return null;
      });
    } catch (Exception e) {
      log.error("execute:: exception caught during edifact job execution", e);
      throw e;
    }
  }

  private String getTenantId(JobExecutionContext jobExecutionContext) {
    String tenantId = jobExecutionContext.getJobDetail().getJobDataMap().getString(TENANT_ID_PARAM);
    if (tenantId == null) {
      throw new IllegalArgumentException(String.format(PARAM_NOT_FOUND_MESSAGE, TENANT_ID_PARAM));
    }
    return tenantId;
  }

  private Job getJob(JobExecutionContext jobExecutionContext) {
    String exportConfigId = jobExecutionContext.getJobDetail().getJobDataMap().getString(EXPORT_CONFIG_ID_PARAM);
    if (exportConfigId == null) {
      throw new IllegalArgumentException(String.format(PARAM_NOT_FOUND_MESSAGE, EXPORT_CONFIG_ID_PARAM));
    }
    try {
      ExportConfig exportConfig = exportTypeBasedConfigManager.getConfigById(exportConfigId);
      return createJob(exportConfig);
    } catch (NotFoundException e) {
      // if configuration cannot be found for scheduled job, it means that it was removed (for ex. by removing
      // the integration) and the job should not be run anymore, so need to delete it from scheduler.
      log.warn("getJob:: configuration id '{}' does not exist anymore. The job will be unscheduled",
        exportConfigId);
      deleteJob(jobExecutionContext);
      throw new SchedulingException(String.format(
        "Configuration id '%s' could not be found. The job was unscheduled", exportConfigId), e);
    }
  }

  private Job createJob(ExportConfig exportConfig) {
    Job job = new Job();
    job.setType(exportConfig.getType());
    job.setIsSystemSource(true);
    job.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
    job.setTenant(exportConfig.getTenant());
    log.info("createJob:: scheduled job assigned '{}'.", job);
    return job;
  }

  private void deleteJob(JobExecutionContext jobExecutionContext) {
    JobKey jobKey = jobExecutionContext.getJobDetail().getKey();
    try {
      jobExecutionContext.getScheduler().deleteJob(jobKey);
      log.info("deleteJob:: job '{}' was deleted from scheduler", jobKey);
    } catch (Exception e) {
      log.warn("deleteJob:: exception deleting job '{}'", jobKey, e);
    }
  }
}
