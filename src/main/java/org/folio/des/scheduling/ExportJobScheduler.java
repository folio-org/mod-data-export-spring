package org.folio.des.scheduling;

import org.folio.des.domain.dto.ExportConfig;

public interface ExportJobScheduler {
  /**
   * Schedule export job based on export config
   *
   * @param exportConfig export config
   */
  void scheduleExportJob(ExportConfig exportConfig);

}
