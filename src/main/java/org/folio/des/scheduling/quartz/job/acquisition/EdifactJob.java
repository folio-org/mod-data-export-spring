package org.folio.des.scheduling.quartz.job.acquisition;

import static org.folio.des.scheduling.quartz.QuartzConstants.EXPORT_CONFIG_ID_PARAM;
import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import org.folio.des.builder.job.JobCommandSchedulerBuilder;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.quartz.JobExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class EdifactJob implements org.quartz.Job {
  private final ExportTypeBasedConfigManager exportTypeBasedConfigManager;
  private final JobExecutionService jobExecutionService;
  private final JobService jobService;
  private final JobCommandSchedulerBuilder jobSchedulerCommandBuilder;
  private final FolioExecutionContextHelper contextHelper;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    try {
      String tenantId = getTenantId(jobExecutionContext);
      try (var context = new FolioExecutionContextSetter(contextHelper.getFolioExecutionContext(tenantId))) {
        Job job = getJob(jobExecutionContext);
        Job resultJob = jobService.upsertAndSendToKafka(job, false);
        log.info("execute:: configured task saved in DB jobId: {}", resultJob.getId());
        if (resultJob.getId() != null) {
          var jobCommand = jobSchedulerCommandBuilder.buildJobCommand(resultJob);
          jobExecutionService.sendJobCommand(jobCommand);
          log.info("execute:: configured task scheduled and sent to kafka for jobId: {}", resultJob.getId());
        }
      }
    } catch (Exception e) {
      // TODO probably just log?
      log.error("execute:: exception caught during edifact job execution", e);
      throw e;
    }
  }

  private String getTenantId(JobExecutionContext jobExecutionContext) {
    String tenantId = jobExecutionContext.getJobDetail().getJobDataMap().getString(TENANT_ID_PARAM);
    if (tenantId == null) {
      throw new IllegalArgumentException("'tenantId' param is missing in the jobExecutionContext");
    }
    return tenantId;
  }

  private Job getJob(JobExecutionContext jobExecutionContext) {
    String exportConfigId = jobExecutionContext.getJobDetail().getJobDataMap().getString(EXPORT_CONFIG_ID_PARAM);
    if (exportConfigId == null) {
      throw new IllegalArgumentException("'exportConfigId' param is missing in the jobExecutionContext");
    }
    return createJob(exportTypeBasedConfigManager.getConfigById(exportConfigId));
  }

  private Job createJob(ExportConfig exportConfig) {
    Job job = new Job();
    job.setType(exportConfig.getType());
    job.setIsSystemSource(true);
    job.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
    job.setTenant(exportConfig.getTenant());
    log.info("Scheduled job assigned {}.", job);
    return job;
  }
}
