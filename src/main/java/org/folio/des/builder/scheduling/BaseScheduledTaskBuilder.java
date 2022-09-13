package org.folio.des.builder.scheduling;

import java.util.Date;
import java.util.Optional;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.folio.des.service.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.validation.constraints.NotNull;

@Log4j2
@RequiredArgsConstructor
public class BaseScheduledTaskBuilder implements ScheduledTaskBuilder {
  protected final JobService jobService;
  protected final FolioExecutionContextHelper contextHelper;

  @Override
  public Optional<ScheduledTask> buildTask(ExportConfig exportConfig) {
    return createScheduledJob(exportConfig).map(job -> new ScheduledTask(buildRunnableTask(job), job));
  }

  @NotNull
  protected Runnable buildRunnableTask(Job job) {
    return () -> {
      var current = new Date();
      log.info("configureTasks attempt to execute at: {}: is module registered: {} ", current, contextHelper.isModuleRegistered());
      if (contextHelper.isModuleRegistered()) {
        contextHelper.initScope(job.getTenant());

        Job resultJob = jobService.upsertAndSendToKafka(job, true);
        log.info("configureTasks executed for jobId: {} at: {}", resultJob.getId(), current);
        contextHelper.finishContext();
      }
    };
  }

  protected Optional<Job> createScheduledJob(ExportConfig exportConfig) {
    Job scheduledJob;
    if (exportConfig == null) {
      return Optional.empty();
    } else {
      scheduledJob = new Job();
      scheduledJob.setType(exportConfig.getType());
      scheduledJob.setIsSystemSource(true);
      scheduledJob.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
      scheduledJob.setTenant(exportConfig.getTenant());
      log.info("Scheduled job assigned {}.", scheduledJob);
      return Optional.of(scheduledJob);
    }
  }
}
