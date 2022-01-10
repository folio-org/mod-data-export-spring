package org.folio.des.builder.scheduling;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.scheduling.acquisition.ScheduleUtil;
import org.folio.des.service.JobService;
import org.jetbrains.annotations.NotNull;

import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
public class EdifactScheduledTaskBuilder extends BaseScheduledTaskBuilder {
  private final AcqSchedulingProperties acqSchedulingProperties;

  public EdifactScheduledTaskBuilder(JobService jobService, FolioExecutionContextHelper contextHelper,
                                     AcqSchedulingProperties acqSchedulingProperties) {
    super(jobService, contextHelper);
    this.acqSchedulingProperties = acqSchedulingProperties;
  }

  @NotNull
  @Override
  protected Runnable buildRunnableTask(Job job) {
    return () -> {
      log.info("Build task : is module registered: {} ", contextHelper.isModuleRegistered());
      boolean isJobScheduleAllowed = ScheduleUtil.isJobScheduleAllowed(acqSchedulingProperties.isRunOnlyIfModuleRegistered(),
                                                                       contextHelper.isModuleRegistered());
      if (isJobScheduleAllowed) {
        Job resultJob = jobService.upsert(job);
        log.info("Configured task executed for jobId: {}", resultJob.getId());
      }
    };
  }
}
