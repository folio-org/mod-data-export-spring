package org.folio.des.scheduling.quartz.converter.acquisition;

import static org.folio.des.scheduling.quartz.QuartzConstants.EXPORT_CONFIG_ID_PARAM;
import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.scheduling.quartz.converter.ExportConfigToJobDetailsConverter;
import org.folio.des.scheduling.quartz.job.ScheduledJobDetails;
import org.folio.des.scheduling.quartz.job.acquisition.EdifactJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ExportConfigToEdifactJobDetailConverter implements ExportConfigToJobDetailsConverter {

  @Override
  public ScheduledJobDetails convert(ExportConfig exportConfig, JobKey jobKey) {
    if (exportConfig == null) {
      return null;
    }
    Job job = createJob(exportConfig);
    JobDetail jobDetail = JobBuilder.newJob(EdifactJob.class)
      //.usingJobData(JOB_PARAM, objectMapper.writeValueAsString(createJob(exportConfig)))
      .usingJobData(TENANT_ID_PARAM, exportConfig.getTenant())
      .usingJobData(EXPORT_CONFIG_ID_PARAM, exportConfig.getId())
      .withIdentity(jobKey)
      .build();
    return new ScheduledJobDetails(job, jobDetail);
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
