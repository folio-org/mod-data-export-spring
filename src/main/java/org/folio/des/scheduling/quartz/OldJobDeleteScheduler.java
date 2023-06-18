package org.folio.des.scheduling.quartz;

import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import java.util.TimeZone;
import java.util.UUID;

import org.folio.des.scheduling.quartz.job.bursar.OldDeleteJob;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class OldJobDeleteScheduler {
  private final Scheduler scheduler;

  public OldJobDeleteScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Transactional
  public void scheduleOldJobDeletion(String tenantId) {
    JobKey jobKey = getDeleteJobKey(tenantId);
    JobDetail jobDetail = getDeleteJobDetail(tenantId, jobKey);
    try {
      if (!checkIfExists(jobKey)) {
        scheduler.scheduleJob(jobDetail, getDeleteJobTrigger(tenantId));
        log.info("scheduleOldJobDeletion:: Delete Old Job scheduled successfully");
      }
    } catch (Exception e) {
      log.warn("scheduleOldJobDeletion:: scheduling failure", e);
    }
  }

  @Transactional
  public void removeJobs(String tenantId) {
    JobKey jobKey = getDeleteJobKey(tenantId);
    try {
      scheduler.deleteJob(jobKey);
      log.info("deleteOldJobDeletionScheduler:: Old Job scheduler deleted successfully");
    } catch (SchedulerException e) {
      log.warn("removeOldJobDeletionScheduler:: scheduling failure", e);
    }
  }

  private boolean checkIfExists(JobKey jobKey) throws SchedulerException {
    return scheduler.checkExists(jobKey);
  }

  private Trigger getDeleteJobTrigger(String tenantId) {
    return TriggerBuilder.newTrigger()
      .withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
        .withIntervalInDays(1)
        .inTimeZone(TimeZone.getTimeZone("UTC"))
        .preserveHourOfDayAcrossDaylightSavings(true)
        .withMisfireHandlingInstructionDoNothing())
      .withIdentity(UUID.randomUUID().toString(), getGroup(tenantId))
      .startNow()
      .build();
  }

  private JobDetail getDeleteJobDetail(String tenantId, JobKey jobKey) {
    return JobBuilder.newJob(OldDeleteJob.class)
      .usingJobData(TENANT_ID_PARAM, tenantId)
      .withIdentity(jobKey)
      .build();
  }

  private JobKey getDeleteJobKey(String tenantId) {
    return JobKey.jobKey(tenantId, getGroup(tenantId));
  }

  private String getGroup(String tenantId) {
    return tenantId + "_" + QuartzConstants.EXPORT_DELETE_GROUP_NAME;
  }
}
