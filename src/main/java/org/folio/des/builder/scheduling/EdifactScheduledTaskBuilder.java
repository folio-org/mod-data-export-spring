package org.folio.des.builder.scheduling;

import java.util.Optional;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.scheduling.acquisition.ScheduleUtil;
import org.folio.des.service.JobService;
import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class EdifactScheduledTaskBuilder implements ScheduledTaskBuilder {
  private final JobService jobService;
  private final FolioExecutionContextHelper contextHelper;
  private final AcqSchedulingProperties acqSchedulingProperties;

  @Override
  public Optional<ScheduledTask> buildTask(ExportConfig exportConfig) {
    return createScheduledJob(exportConfig).map(job -> new ScheduledTask(buildRunnableTask(job), job));
  }

  @NotNull
  private Runnable buildRunnableTask(Job job) {
    return () -> {
      log.info("Build task : is module registered: {} ", contextHelper.isModuleRegistered());
      boolean isJobScheduleAllowed = ScheduleUtil.isJobScheduleAllowed(acqSchedulingProperties.isRunOnlyIfModuleRegistered(),
                                                                       contextHelper.isModuleRegistered());
      if (isJobScheduleAllowed) {
        jobService.upsert(job);
        log.info("Configured task executed for jobId: {} at: {}", job.getId());
      }
    };
  }

  private Optional<Job> createScheduledJob(ExportConfig exportConfig) {
    Job scheduledJob;
    if (exportConfig == null) {
      return Optional.empty();
    } else {
      scheduledJob = new Job();
      scheduledJob.setType(exportConfig.getType());
      scheduledJob.setIsSystemSource(true);
      scheduledJob.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
      log.info("Scheduled job assigned {}.", scheduledJob);
      return Optional.of(scheduledJob);
    }
  }
}
