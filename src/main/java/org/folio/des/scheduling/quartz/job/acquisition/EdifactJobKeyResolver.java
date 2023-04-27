package org.folio.des.scheduling.quartz.job.acquisition;

import java.util.Optional;
import java.util.UUID;

import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.job.JobKeyResolver;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

@Component
public class EdifactJobKeyResolver implements JobKeyResolver {
  @Override
  public JobKey resolve(ExportConfig exportConfig) {
    if (exportConfig == null) {
      return null;
    }

    String jobId = Optional.ofNullable(exportConfig.getExportTypeSpecificParameters())
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .map(VendorEdiOrdersExportConfig::getEdiSchedule)
      .map(EdiSchedule::getScheduleParameters)
      .map(ScheduleParameters::getId)
      .map(UUID::toString)
      .orElse(exportConfig.getId());

    if (jobId == null) {
      throw new IllegalArgumentException("Export config does not contain schedule id or export config id");
    }
    return JobKey.jobKey(jobId, QuartzConstants.EDIFACT_ORDERS_EXPORT_GROUP_NAME);
  }
}
