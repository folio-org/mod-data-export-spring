package org.folio.des.scheduling.quartz;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.exceptions.SchedulingException;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.quartz.converter.ExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.JobKeyResolver;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ExportJobScheduler} quartz implementation
 */
@RequiredArgsConstructor
@Log4j2
public class QuartzExportJobScheduler implements ExportJobScheduler {
  private final Scheduler scheduler;
  private final Converter<ExportConfig, ExportTrigger> triggerConverter;
  private final ExportConfigToJobDetailConverter jobDetailConverter;
  private final JobKeyResolver jobKeyResolver;

  @Override
  @Transactional
  public List<Job> scheduleExportJob(ExportConfig exportConfig) {
    if (exportConfig == null) {
      return Collections.emptyList();
    }

    try {
      JobKey jobKey = jobKeyResolver.resolve(exportConfig);
      if (scheduleExists(jobKey)) {
        rescheduleJob(jobKey, exportConfig);
      } else {
        scheduleJob(jobKey, exportConfig);
      }
      // job creation does not happen on quartz scheduling, so just return empty list
      return Collections.emptyList();
    } catch (Exception e) {
      log.warn("Error during scheduling for config id {}", exportConfig.getId(), e);
      throw new SchedulingException("Error during scheduling", e);
    }
  }

  private void scheduleJob(JobKey jobKey, ExportConfig exportConfig) throws SchedulerException {
    ExportTrigger exportTrigger = triggerConverter.convert(exportConfig);
    if (exportTrigger == null) {
      log.warn("scheduleJob:: null trigger created for config id  {}", exportConfig.getId());
      return;
    }
    if (!exportTrigger.isDisabled()) {
      scheduleJob(jobKey, exportTrigger, exportConfig);
      log.info("scheduleJob:: job {} scheduled for config id {}", jobKey, exportConfig.getId());
    } else {
      log.info("scheduleJob:: schedule is disabled for config id {}", exportConfig.getId());
    }
  }

  private void rescheduleJob(JobKey jobKey, ExportConfig exportConfig) throws SchedulerException {
    ExportTrigger exportTrigger = triggerConverter.convert(exportConfig);
    if (exportTrigger == null) {
      log.warn("rescheduleJob:: null trigger created for config id  {}", exportConfig.getId());
      return;
    }
    if (exportTrigger.isDisabled()) {
      scheduler.deleteJob(jobKey);
      log.info("rescheduleJob:: job {} deleted for config id {}", jobKey, exportConfig.getId());
    } else {
      // remove existing and schedule updated job
      scheduler.deleteJob(jobKey);
      scheduleJob(jobKey, exportTrigger, exportConfig);
      log.info("rescheduleJob:: job {} rescheduled for config id {}", jobKey, exportConfig.getId());
    }
  }

  private boolean scheduleExists(JobKey jobKey) throws SchedulerException {
    return scheduler.checkExists(jobKey);
  }

  private void scheduleJob(JobKey jobKey, ExportTrigger exportTrigger, ExportConfig exportConfig)
    throws SchedulerException {

    Set<Trigger> triggers = exportTrigger.triggers();
    if (CollectionUtils.isNotEmpty(triggers)) {
      JobDetail jobDetail = jobDetailConverter.convert(exportConfig, jobKey);
      scheduler.scheduleJob(jobDetail, triggers, false);
      triggers.forEach(trigger -> log.info("scheduleJob:: job {} was scheduled with trigger {}. " +
        "Next execution time is: {}", jobKey, trigger.getKey(), trigger.getFireTimeAfter(new Date())));
    }
  }
}
