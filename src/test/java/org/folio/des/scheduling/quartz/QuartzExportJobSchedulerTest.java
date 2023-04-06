package org.folio.des.scheduling.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;

import lombok.extern.log4j.Log4j2;

@SpringBootTest(properties = {
  "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
  "folio.quartz.enabled=true"})
class QuartzExportJobSchedulerTest extends BaseTest {

  private static final String SCHEDULE_ID = "scheduleId_" + UUID.randomUUID();

  @Autowired
  private Scheduler scheduler;
  private QuartzExportJobScheduler quartzExportJobScheduler;

  @BeforeEach
  void init() {
    quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler,
      new TestTriggerConverter(), new TestJobDetailConverter());
  }

  @AfterEach
  void afterEach() throws SchedulerException {
    scheduler.clear();
  }

  @Test
  void testSchedule() throws SchedulerException {
    TestingJobListener jobListener = registerTestingJobListener();
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, true);
    quartzExportJobScheduler.scheduleExportJob(config);

    assertTrue(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(1, schedulerListener.getJobsAddedCount());
    assertEquals(1, schedulerListener.getJobsScheduledCount());

    await().pollDelay(1, TimeUnit.SECONDS).timeout(5, TimeUnit.SECONDS).untilAsserted(
      () -> assertEquals(1, jobListener.getJobsExecutedCount()));
  }

  @Test
  void testUnschedule() throws SchedulerException {
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, true);
    quartzExportJobScheduler.scheduleExportJob(config);

    // scheduleExportJob with disabled scheduling for existing id should result in job unscheduling
    ExportConfig disabledConfig = buildExportConfig(SCHEDULE_ID, false);
    quartzExportJobScheduler.scheduleExportJob(disabledConfig);

    assertFalse(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(1, schedulerListener.getJobsAddedCount());
    assertEquals(1, schedulerListener.getJobsScheduledCount());
    assertEquals(1, schedulerListener.getJobsDeletedCount());
  }

  @Test
  void testReschedule() throws SchedulerException {
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, true);
    quartzExportJobScheduler.scheduleExportJob(config);

    // scheduleExportJob with enabled scheduling for existing id should result in job rescheduling
    ExportConfig reschedulingConfig = buildExportConfig(SCHEDULE_ID, true);
    quartzExportJobScheduler.scheduleExportJob(reschedulingConfig);

    assertTrue(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(1, schedulerListener.getJobsAddedCount());
    assertEquals(2, schedulerListener.getJobsScheduledCount());
    assertEquals(1, schedulerListener.getJobsUnscheduledCount());
  }

  @Test
  void testScheduleForDisabledConfig() throws SchedulerException {
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, false);
    quartzExportJobScheduler.scheduleExportJob(config);

    assertFalse(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(0, schedulerListener.getJobsScheduledCount());
  }

  private ExportConfig buildExportConfig(String configId, boolean isEnabled) {
    return new ExportConfig()
      .id(configId)
      .scheduleTime("2024-04-24T11:00Z")
      .schedulePeriod(isEnabled ? SchedulePeriodEnum.DAY : SchedulePeriodEnum.NONE)
      .scheduleFrequency(2);
  }

  private TestingSchedulerListener registerTestingSchedulerListener() throws SchedulerException {
    TestingSchedulerListener schedulerListener = new TestingSchedulerListener();
    scheduler.getListenerManager().addSchedulerListener(schedulerListener);
    return schedulerListener;
  }

  private TestingJobListener registerTestingJobListener() throws SchedulerException {
    TestingJobListener jobListener = new TestingJobListener();
    scheduler.getListenerManager().addJobListener(jobListener);
    return jobListener;
  }

  private static class TestTriggerConverter implements Converter<ExportConfig, Trigger> {
    @Override
    public Trigger convert(ExportConfig exportConfig) {
      return TriggerBuilder
        .newTrigger()
        .withIdentity(exportConfig.getId())
        .withSchedule(SimpleScheduleBuilder
          .simpleSchedule()
          .withRepeatCount(0))
        .startAt(Date.from(Instant.now().plus(2, ChronoUnit.SECONDS)))
        .build();
    }
  }

  private static class TestJobDetailConverter implements Converter<ExportConfig, JobDetail> {
    @Override
    public JobDetail convert(ExportConfig exportConfig) {
      return JobBuilder
        .newJob(DraftJob.class)
        .withIdentity(exportConfig.getId())
        .build();
    }
  }

  @Log4j2
  private static class DraftJob implements org.quartz.Job {
    @Override
    public void execute(JobExecutionContext context) {
      log.info("job {} is executing", context.getJobDetail().getKey());
    }
  }

  private static class TestingJobListener extends JobListenerSupport {
    private final AtomicInteger jobsExecutedCount = new AtomicInteger();

    @Override
    public String getName() {
      return "TestingJobListener";
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
      jobsExecutedCount.incrementAndGet();
    }

    public int getJobsExecutedCount() {
      return jobsExecutedCount.get();
    }
  }

  private static class TestingSchedulerListener extends SchedulerListenerSupport {
    private final AtomicInteger jobsAddedCount = new AtomicInteger();
    private final AtomicInteger jobsDeletedCount = new AtomicInteger();
    private final AtomicInteger jobsUnscheduledCount = new AtomicInteger();
    private final AtomicInteger jobsScheduledCount = new AtomicInteger();

    @Override
    public void jobDeleted(JobKey jobKey) {
      jobsDeletedCount.incrementAndGet();
    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
      jobsUnscheduledCount.incrementAndGet();
    }

    @Override
    public void jobAdded(JobDetail jobDetail) {
      jobsAddedCount.incrementAndGet();
    }

    @Override
    public void jobScheduled(Trigger trigger) {
      jobsScheduledCount.incrementAndGet();
    }

    public int getJobsAddedCount() {
      return jobsAddedCount.get();
    }

    public int getJobsDeletedCount() {
      return jobsDeletedCount.get();
    }

    public int getJobsUnscheduledCount() {
      return jobsUnscheduledCount.get();
    }

    public int getJobsScheduledCount() {
      return jobsScheduledCount.get();
    }
  }
}
