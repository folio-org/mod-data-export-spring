package org.folio.des.scheduling.quartz.converter.acquisition;

import org.folio.des.scheduling.quartz.converter.BaseExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.acquisition.EdifactJob;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ExportConfigToEdifactJobDetailConverter extends BaseExportConfigToJobDetailConverter {
  public ExportConfigToEdifactJobDetailConverter() {
    super(EdifactJob.class);
  }
}
