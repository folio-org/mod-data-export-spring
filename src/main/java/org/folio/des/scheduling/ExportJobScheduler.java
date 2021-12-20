package org.folio.des.scheduling;

import java.util.List;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;

public interface ExportJobScheduler {
  List<Job> scheduleExportJob(ExportConfig exportConfig);
  void initAllScheduledJob();
}
