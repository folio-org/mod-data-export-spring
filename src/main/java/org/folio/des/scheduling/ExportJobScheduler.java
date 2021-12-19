package org.folio.des.scheduling;

import org.folio.des.domain.dto.ExportConfig;

public interface ExportJobScheduler {
  void scheduleExportJob(ExportConfig exportConfig);
  void initAllScheduledJob();
}
