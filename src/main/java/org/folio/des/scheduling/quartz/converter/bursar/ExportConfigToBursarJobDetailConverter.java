package org.folio.des.scheduling.quartz.converter.bursar;

import org.folio.des.scheduling.quartz.converter.BaseExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarJob;
import org.springframework.stereotype.Component;

@Component
public class ExportConfigToBursarJobDetailConverter extends BaseExportConfigToJobDetailConverter {
  public ExportConfigToBursarJobDetailConverter() {
    super(BursarJob.class);
  }
}
