package org.folio.des.scheduling.quartz.converter.bursar;

import org.folio.des.scheduling.quartz.converter.BaseExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarJob;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ExportConfigToBursarJobDetailConverter extends BaseExportConfigToJobDetailConverter {
  public ExportConfigToBursarJobDetailConverter() {
    super(BursarJob.class);
  }
}
