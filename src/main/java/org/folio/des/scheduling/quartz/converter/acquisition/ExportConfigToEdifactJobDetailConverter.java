package org.folio.des.scheduling.quartz.converter.acquisition;

import org.folio.des.scheduling.quartz.converter.BaseExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.acquisition.EdifactJob;
import org.springframework.stereotype.Component;

@Component
public class ExportConfigToEdifactJobDetailConverter extends BaseExportConfigToJobDetailConverter {
  public ExportConfigToEdifactJobDetailConverter() {
    super(EdifactJob.class);
  }
}
