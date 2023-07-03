package org.folio.des.scheduling.bursar;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.exceptions.SchedulingException;
import org.folio.des.scheduling.quartz.QuartzExportJobScheduler;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarJobDetailConverter;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarTriggerConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarJobKeyResolver;
import org.quartz.Scheduler;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class BursarExportScheduler {
  private final QuartzExportJobScheduler quartzExportJobScheduler;

  public BursarExportScheduler(ExportConfigToBursarTriggerConverter exportConfigToBursarTriggerConverter, BursarJobKeyResolver bursarJobKeyResolver, ExportConfigToBursarJobDetailConverter exportConfigToBursarJobDetailConverter, Scheduler scheduler) {

    this.quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler,
      exportConfigToBursarTriggerConverter, exportConfigToBursarJobDetailConverter, bursarJobKeyResolver);
  }

  public void scheduleBursarJob(ExportConfig exportConfig) {
    if (exportConfig == null) {
      log.warn("scheduleBursarJob:: exportConfig is null");
      return;
    }
    try {
      quartzExportJobScheduler.scheduleExportJob(exportConfig);
    } catch (Exception e) {
      log.warn("scheduleBursarJob:: Error while scheduling BursarJob: ", e);
      throw new SchedulingException("Error during Bursar scheduling", e);
    }
  }
}
