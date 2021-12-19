package org.folio.des.builder.scheduling;

import java.util.Date;
import java.util.Optional;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class EdifactScheduledTaskBuilder implements ScheduledTaskBuilder {
  private final JobService jobService;
  private final FolioExecutionContextHelper contextHelper;

  @Override
  public Runnable buildTask(ExportConfig exportConfig) {
    return () -> {
      var current = new Date();
      log.info("configureTasks attempt to execute at: {}: is module registered: {} ", current, contextHelper.isModuleRegistered());
      Optional<Job> scheduledJob = createScheduledJob(exportConfig);
      if (scheduledJob.isPresent() && contextHelper.isModuleRegistered()) {
        contextHelper.initScope();
        jobService.upsert(scheduledJob.get());
        log.info("configureTasks executed for jobId: {} at: {}", scheduledJob.get().getId(), current);
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
