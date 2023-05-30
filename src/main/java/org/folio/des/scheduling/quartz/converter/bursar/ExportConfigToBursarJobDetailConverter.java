package org.folio.des.scheduling.quartz.converter.bursar;

import static org.folio.des.scheduling.quartz.QuartzConstants.EXPORT_CONFIG_ID_PARAM;
import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.quartz.converter.ExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ExportConfigToBursarJobDetailConverter implements ExportConfigToJobDetailConverter {

  @Override
  public JobDetail convert(ExportConfig exportConfig, JobKey jobKey) {

    if (exportConfig == null) {
      return null;
    }
    return JobBuilder.newJob(BursarJob.class)
      .usingJobData(TENANT_ID_PARAM, exportConfig.getTenant())
      .usingJobData(EXPORT_CONFIG_ID_PARAM, exportConfig.getId())
      .withIdentity(jobKey)
      .build();
  }
}
