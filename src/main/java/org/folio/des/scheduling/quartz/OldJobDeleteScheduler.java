package org.folio.des.scheduling.quartz;

import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.folio.des.scheduling.quartz.job.bursar.OldDeleteJob;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class OldJobDeleteScheduler {
  private final Scheduler scheduler;
  private final String timeZone;

  public OldJobDeleteScheduler(Scheduler scheduler, @Value("${folio.quartz.bursar.timeZone}") String timeZone) {
    this.scheduler = scheduler;
    this.timeZone = timeZone;
  }

  @Transactional
  public void scheduleOldJobDeletion(String tenantId) {
    JobKey jobKey = getDeleteJobKey(tenantId);
    JobDetail jobDetail = getDeleteJobDetail(tenantId, jobKey);
    try {
      if (scheduler.checkExists(jobKey)) {
        scheduler.deleteJob(jobKey);
        scheduler.scheduleJob(jobDetail, Set.of(getDeleteJobTrigger(tenantId)), false);
      } else {
        scheduler.scheduleJob(jobDetail, Set.of(getDeleteJobTrigger(tenantId)), false);
      }
    } catch (Exception e) {
      log.warn("scheduleOldJobDeletion:: scheduling failure", e);
    }
  }

  private Trigger getDeleteJobTrigger(String tenantId) {
    return TriggerBuilder.newTrigger()
      .withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
        .withIntervalInDays(1)
        .inTimeZone(TimeZone.getTimeZone(timeZone))
        .preserveHourOfDayAcrossDaylightSavings(true)
        .withMisfireHandlingInstructionDoNothing())
      .withIdentity(UUID.randomUUID().toString(), getGroup(tenantId))
      .startNow()
      .build();
  }

  public JobDetail getDeleteJobDetail(String tenantId, JobKey jobKey) {
    return JobBuilder.newJob(OldDeleteJob.class)
      .usingJobData(TENANT_ID_PARAM, tenantId)
      .withIdentity(jobKey)
      .build();
  }

  public JobKey getDeleteJobKey(String tenantId) {
    return JobKey.jobKey(tenantId, getGroup(tenantId));
  }

  private String getGroup(String tenantId) {
    return tenantId + "_" + QuartzConstants.EXPORT_DELETE_GROUP_NAME;
  }
}
