package org.folio.des.scheduling.quartz.job.bursar;

import static org.folio.des.scheduling.quartz.QuartzConstants.EXPORT_CONFIG_ID_PARAM;
import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import java.util.Date;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.exceptions.SchedulingException;
import org.folio.des.service.JobService;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class BursarJob implements org.quartz.Job {
  private final FolioExecutionContextHelper contextHelper;
  private final ExportTypeBasedConfigManager exportTypeBasedConfigManager;
  private final JobService jobService;
  private static final String PARAM_NOT_FOUND_MESSAGE = "'%s' param is missing in the jobExecutionContext";

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    String tenantId = getTenantId(jobExecutionContext);
    var current = new Date();

    try (var context = new FolioExecutionContextSetter(contextHelper.getFolioExecutionContext(tenantId))) {
      Job scheduledJob = getJob(jobExecutionContext);
      Job resultJob = jobService.upsertAndSendToKafka(scheduledJob, true);
      log.info("execute:: configureTasks executed for jobId: {} at: {}", resultJob.getId(), current);
    }
  }

  private Job getJob(JobExecutionContext jobExecutionContext) {
    String exportConfigId = jobExecutionContext.getJobDetail().getJobDataMap().getString(EXPORT_CONFIG_ID_PARAM);
    if (exportConfigId == null) {
      throw new IllegalArgumentException(String.format(PARAM_NOT_FOUND_MESSAGE, EXPORT_CONFIG_ID_PARAM));
    }

    try {
      ExportConfig exportConfig = exportTypeBasedConfigManager.getConfigById(exportConfigId);
      return createScheduledJob(exportConfig);

    } catch (NotFoundException e) {
      log.warn("getJob:: configuration id '{}' does not exist anymore. The job will be unscheduled",
        exportConfigId);
      deleteJob(jobExecutionContext);
      throw new SchedulingException(String.format(
        "Configuration id '%s' could not be found. The job was unscheduled", exportConfigId), e);
    }
  }

  private String getTenantId(JobExecutionContext jobExecutionContext) {
    String tenantId = jobExecutionContext.getJobDetail().getJobDataMap().getString(TENANT_ID_PARAM);
    if (tenantId == null) {
      throw new IllegalArgumentException(String.format(PARAM_NOT_FOUND_MESSAGE, TENANT_ID_PARAM));
    }
    return tenantId;
  }

  private Job createScheduledJob(ExportConfig exportConfig) {
    Job scheduledJob = new Job();
    scheduledJob.setType(exportConfig.getType());
    scheduledJob.setIsSystemSource(true);
    scheduledJob.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
    scheduledJob.setTenant(exportConfig.getTenant());
    log.info("createScheduledJob:: Scheduled job assigned {}", scheduledJob);
    return scheduledJob;
  }

  private void deleteJob(JobExecutionContext jobExecutionContext) {
    JobKey jobKey = jobExecutionContext.getJobDetail().getKey();
    try {
      jobExecutionContext.getScheduler().deleteJob(jobKey);
      log.info("deleteJob:: bursarJob '{}' was deleted from scheduler", jobKey);
    } catch (Exception e) {
      log.warn("deleteJob:: exception deleting job '{}'", jobKey, e);
    }
  }
}
