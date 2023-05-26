package org.folio.des.scheduling;

import java.util.List;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;

public interface ExportJobScheduler {
  /**
   * Schedule export job based on export config
   *
   * @param exportConfig export config
   * @return list of jobs
   */
  List<Job> scheduleExportJob(ExportConfig exportConfig);

}
