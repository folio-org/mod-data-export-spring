package org.folio.des.scheduling;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.exceptions.SchedulingException;
import org.folio.des.scheduling.quartz.QuartzExportJobScheduler;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarDeleteJobDetailConverter;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarDeleteTriggerConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarDeleteJobKeyResolver;
import org.quartz.Scheduler;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class BursarExportDeleteScheduler {
  private final QuartzExportJobScheduler quartzExportJobScheduler;

  public BursarExportDeleteScheduler(ExportConfigToBursarDeleteTriggerConverter exportConfigToBursarDeleteTriggerConverter, BursarDeleteJobKeyResolver bursarDeleteJobKeyResolver, ExportConfigToBursarDeleteJobDetailConverter exportConfigToBursarDeleteJobDetailConverter, Scheduler scheduler) {

    this.quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler,
      exportConfigToBursarDeleteTriggerConverter, exportConfigToBursarDeleteJobDetailConverter, bursarDeleteJobKeyResolver);
  }

  public void scheduleBursarDeleteJob(ExportConfig exportConfig) {
    try {
      quartzExportJobScheduler.scheduleExportJob(exportConfig);
    } catch (Exception e) {
      log.warn("scheduleBursarDeleteJob:: Error while scheduling BursarDeleteJob: ", e);
      throw new SchedulingException("Error during BursarDelete scheduling", e);
    }
  }
}
