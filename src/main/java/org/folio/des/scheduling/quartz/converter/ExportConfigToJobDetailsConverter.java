package org.folio.des.scheduling.quartz.converter;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.quartz.job.ScheduledJobDetails;
import org.quartz.JobKey;

public interface ExportConfigToJobDetailsConverter {
  ScheduledJobDetails convert(ExportConfig exportConfig, JobKey jobKey);
}
