package org.folio.des.scheduling.quartz;

import java.util.Collections;
import java.util.List;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.Job;
import org.folio.des.scheduling.ExportJobScheduler;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.core.convert.converter.Converter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Draft of quartz {@link ExportJobScheduler} implementation
 */
@RequiredArgsConstructor
@Log4j2
public class QuartzExportJobScheduler implements ExportJobScheduler {
  private final Scheduler scheduler;
  // will be Converter<ExportConfig, List<Trigger>> if it'll be needed to set up several triggers for same job
  private final Converter<ExportConfig, Trigger> triggerConverter;
  private final Converter<ExportConfig, JobDetail> jobDetailConverter;

  @Override
  @SneakyThrows
  public List<Job> scheduleExportJob(ExportConfig exportConfig) {
    if (scheduleExists(exportConfig)) {
      rescheduleJob(exportConfig);
    } else {
      scheduleJob(exportConfig);
    }
    // there should be the list of jobs created based on
    // exportConfig and scheduled/rescheduled
    return Collections.emptyList();
  }

  private void scheduleJob(ExportConfig exportConfig) throws SchedulerException {
    if (!isDisabledSchedule(exportConfig)) {
      Trigger trigger = buildTrigger(exportConfig);
      JobDetail jobDetail = buildJobDetail(exportConfig);
      scheduler.scheduleJob(jobDetail, trigger);
      log.info("scheduleJob:: job {} with trigger {} scheduled for config id {}", () -> jobDetail.getKey(),
        () -> trigger.getKey(), () -> exportConfig.getId());
    } else {
      log.info("scheduleJob:: schedule is disabled for config id {}", () -> exportConfig.getId());
    }
  }

  private void rescheduleJob(ExportConfig exportConfig) throws SchedulerException {
    if (isDisabledSchedule(exportConfig)) {
      JobKey jobKey = getJobKey(exportConfig);
      scheduler.deleteJob(jobKey);
      log.info("rescheduleJob:: job {} deleted for config id {}", () -> jobKey, () -> exportConfig.getId());
    } else {
      TriggerKey triggerKey = getTriggerKey(exportConfig);
      scheduler.rescheduleJob(triggerKey, buildTrigger(exportConfig));
      log.info("rescheduleJob:: trigger {} rescheduled for config id {}", () -> triggerKey, () -> exportConfig.getId());
    }
  }

  private boolean scheduleExists(ExportConfig exportConfig) throws SchedulerException {
    return scheduler.checkExists(getTriggerKey(exportConfig));
  }

  private boolean isDisabledSchedule(ExportConfig exportConfig) {
    SchedulePeriodEnum schedulePeriod = exportConfig.getSchedulePeriod();
    return schedulePeriod == null || SchedulePeriodEnum.NONE == exportConfig.getSchedulePeriod();
  }

  private TriggerKey getTriggerKey(ExportConfig exportConfig) {
    return TriggerKey.triggerKey(exportConfig.getId());
  }

  private JobKey getJobKey(ExportConfig exportConfig) {
    return JobKey.jobKey(exportConfig.getId());
  }

  private JobDetail buildJobDetail(ExportConfig exportConfig) {
    return jobDetailConverter.convert(exportConfig);
  }

  private Trigger buildTrigger(ExportConfig exportConfig) {
    return triggerConverter.convert(exportConfig);
  }
}
