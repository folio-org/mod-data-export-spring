package org.folio.des.config.scheduling;

import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;

public interface SchedulingConfig {
  ExportJobScheduler edifactOrdersExportJobScheduler();

  ExportJobScheduler initEdifactOrdersExportJobScheduler();

  AcqSchedulingProperties acqSchedulingProperties();
}
