package org.folio.des.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.quartz.QuartzExportJobScheduler;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarJobDetailConverter;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarTriggerConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarJobKeyResolver;
import org.quartz.Scheduler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class BursarExportScheduler {


  private final ExportConfigToBursarTriggerConverter exportConfigToBursarTriggerConverter;
  private final BursarJobKeyResolver bursarJobKeyResolver;
  private final ExportConfigToBursarJobDetailConverter exportConfigToBursarJobDetailConverter;
  private final Scheduler scheduler;


  public void scheduleBursarJob(ExportConfig exportConfig) {
    if(exportConfig == null) {
      log.error("exportConfig is null");
      return;
    }
    try {
      QuartzExportJobScheduler quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler,
        exportConfigToBursarTriggerConverter,exportConfigToBursarJobDetailConverter,bursarJobKeyResolver);

        quartzExportJobScheduler.scheduleExportJob(exportConfig);

    } catch (Exception e) {
      log.error("Error while scheduling BursarJob: ",e);
    }
  }
}
