package org.folio.des.scheduling.quartz.converter;

import org.folio.des.domain.dto.ExportConfig;
import org.quartz.JobDetail;
import org.quartz.JobKey;

public interface ExportConfigToJobDetailConverter {
  JobDetail convert(ExportConfig exportConfig, JobKey jobKey);
}
