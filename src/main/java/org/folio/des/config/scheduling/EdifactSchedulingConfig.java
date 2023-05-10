package org.folio.des.config.scheduling;

import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;

public interface EdifactSchedulingConfig {
  ExportJobScheduler edifactOrdersExportJobScheduler();

  ExportJobScheduler initEdifactOrdersExportJobScheduler();

  AcqSchedulingProperties acqSchedulingProperties();
}
