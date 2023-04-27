package org.folio.des.scheduling.quartz;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.quartz.converter.ExportConfigToJobDetailsConverter;
import org.folio.des.scheduling.quartz.job.JobKeyResolver;
import org.folio.des.scheduling.quartz.job.ScheduledJobDetails;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Quartz {@link ExportJobScheduler} implementation
 */
@RequiredArgsConstructor
@Log4j2
public class QuartzExportJobScheduler implements ExportJobScheduler {
  private final Scheduler scheduler;
  private final Converter<ExportConfig, ExportTrigger> triggerConverter;
  private final ExportConfigToJobDetailsConverter jobDetailsConverter;
  private final JobKeyResolver jobKeyResolver;

  @Override
  @SneakyThrows
  @Transactional
  public List<Job> scheduleExportJob(ExportConfig exportConfig) {
    if (exportConfig == null) {
      return Collections.emptyList();
    }

    JobKey jobKey = jobKeyResolver.resolve(exportConfig);

    if (scheduleExists(jobKey)) {
      return rescheduleJob(jobKey, exportConfig);
    } else {
      return scheduleJob(jobKey, exportConfig);
    }
  }

  private List<Job> scheduleJob(JobKey jobKey, ExportConfig exportConfig) throws SchedulerException {
    ExportTrigger exportTrigger = triggerConverter.convert(exportConfig);
    if (!exportTrigger.isDisabled()) {
      var jobs = scheduleJob(jobKey, exportTrigger, exportConfig);
      log.info("scheduleJob:: job {} scheduled for config id {}", jobKey, exportConfig.getId());
      return jobs;
    } else {
      log.info("scheduleJob:: schedule is disabled for config id {}", exportConfig.getId());
      return Collections.emptyList();
    }
  }

  private List<Job> rescheduleJob(JobKey jobKey, ExportConfig exportConfig) throws SchedulerException {
    ExportTrigger exportTrigger = triggerConverter.convert(exportConfig);
    if (exportTrigger.isDisabled()) {
      scheduler.deleteJob(jobKey);
      log.info("rescheduleJob:: job {} deleted for config id {}", jobKey, exportConfig.getId());
      return Collections.emptyList();
    } else {
      // remove existing and schedule updated job
      scheduler.deleteJob(jobKey);
      var jobs = scheduleJob(jobKey, exportTrigger, exportConfig);
      log.info("rescheduleJob:: job {} rescheduled for config id {}", jobKey, exportConfig.getId());
      return jobs;
    }
  }

  private boolean scheduleExists(JobKey jobKey) throws SchedulerException {
    return scheduler.checkExists(jobKey);
  }

  private List<Job> scheduleJob(JobKey jobKey, ExportTrigger exportTrigger, ExportConfig exportConfig)
    throws SchedulerException {

    Set<Trigger> triggers = exportTrigger.triggers();
    if (CollectionUtils.isNotEmpty(triggers)) {
      ScheduledJobDetails scheduledJobDetails = jobDetailsConverter.convert(exportConfig, jobKey);
      scheduler.scheduleJob(scheduledJobDetails.jobDetail(), triggers, false);

      triggers.forEach(trigger -> log.info("scheduleJob: job {} was scheduled with trigger {}. " +
        "Next execution time is: {}", jobKey, trigger.getKey(), trigger.getFireTimeAfter(new Date())));

      return List.of(scheduledJobDetails.job());
    }
    return Collections.emptyList();
  }
}
