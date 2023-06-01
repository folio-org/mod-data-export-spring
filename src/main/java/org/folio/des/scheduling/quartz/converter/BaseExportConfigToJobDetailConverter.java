package org.folio.des.scheduling.quartz.converter;

import static org.folio.des.scheduling.quartz.QuartzConstants.EXPORT_CONFIG_ID_PARAM;
import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import org.folio.des.domain.dto.ExportConfig;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BaseExportConfigToJobDetailConverter implements ExportConfigToJobDetailConverter {

  private final Class<? extends Job> jobClass;

  @Override
  public JobDetail convert(ExportConfig exportConfig, JobKey jobKey) {
    if (exportConfig == null) {
      return null;
    }
    return JobBuilder.newJob(jobClass)
      .usingJobData(TENANT_ID_PARAM, exportConfig.getTenant())
      .usingJobData(EXPORT_CONFIG_ID_PARAM, exportConfig.getId())
      .withIdentity(jobKey)
      .build();
  }
}
