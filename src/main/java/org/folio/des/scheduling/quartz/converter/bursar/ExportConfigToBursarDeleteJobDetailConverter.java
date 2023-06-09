package org.folio.des.scheduling.quartz.converter.bursar;

import org.folio.des.scheduling.quartz.converter.BaseExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarDeleteJob;
import org.springframework.stereotype.Component;

@Component
public class ExportConfigToBursarDeleteJobDetailConverter extends BaseExportConfigToJobDetailConverter {

  public ExportConfigToBursarDeleteJobDetailConverter() {
    super(BursarDeleteJob.class);
  }
}
