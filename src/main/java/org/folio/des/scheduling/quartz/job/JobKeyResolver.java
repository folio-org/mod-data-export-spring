package org.folio.des.scheduling.quartz.job;

import org.folio.des.domain.dto.ExportConfig;
import org.quartz.JobKey;

public interface JobKeyResolver {
  JobKey resolve(ExportConfig exportConfig);
}
