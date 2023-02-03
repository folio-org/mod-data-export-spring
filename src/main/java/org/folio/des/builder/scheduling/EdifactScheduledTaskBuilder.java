package org.folio.des.builder.scheduling;

import jakarta.validation.constraints.NotNull;

import org.folio.des.builder.job.JobCommandSchedulerBuilder;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.Job;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.scheduling.acquisition.ScheduleUtil;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EdifactScheduledTaskBuilder extends BaseScheduledTaskBuilder {
  private final AcqSchedulingProperties acqSchedulingProperties;
  private final JobExecutionService jobExecutionService;
  private final JobCommandSchedulerBuilder jobSchedulerCommandBuilder;

  public EdifactScheduledTaskBuilder(JobService jobService, FolioExecutionContextHelper contextHelper,
                                     AcqSchedulingProperties acqSchedulingProperties,
                                     JobExecutionService jobExecutionService,
                                     JobCommandSchedulerBuilder jobSchedulerCommandBuilder) {
    super(jobService, contextHelper);
    this.acqSchedulingProperties = acqSchedulingProperties;
    this.jobExecutionService = jobExecutionService;
    this.jobSchedulerCommandBuilder = jobSchedulerCommandBuilder;
  }

  @NotNull
  @Override
  protected Runnable buildRunnableTask(Job job) {
    return () -> {
      log.info("Build task : is module registered: {} ", contextHelper.isModuleRegistered());
      boolean isJobScheduleAllowed = ScheduleUtil.isJobScheduleAllowed(acqSchedulingProperties.isRunOnlyIfModuleRegistered(),
                                                                       contextHelper.isModuleRegistered());
      if (isJobScheduleAllowed) {
          contextHelper.initScope(job.getTenant());
          Job resultJob = jobService.upsertAndSendToKafka(job, false);
          log.info("Configured task saved in DB jobId: {}", resultJob.getId());
          if (resultJob.getId() != null) {
            var jobCommand = jobSchedulerCommandBuilder.buildJobCommand(resultJob);
            jobExecutionService.sendJobCommand(jobCommand);
            log.info("Configured task scheduled and KafkaTopic sent for jobId: {}", resultJob.getId());
          }
          contextHelper.finishContext();
      }
    };
  }
}
