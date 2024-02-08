package org.folio.des.scheduling.quartz;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.scheduling.quartz.converter.ExportConfigToJobDetailConverter;
import org.folio.des.scheduling.quartz.job.JobKeyResolver;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
  "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
})
@DirtiesContext
class QuartzExportJobSchedulerTest extends BaseTest {

  private static final String SCHEDULE_ID = "scheduleId_" + UUID.randomUUID();
  private QuartzExportJobScheduler quartzExportJobScheduler;

  @BeforeEach
  void init() {
    quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler, new TestTriggerConverter(),
      new TestJobDetailConverter(), new TestJobKeyResolver());
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

    await().pollDelay(1, TimeUnit.SECONDS).timeout(180, TimeUnit.SECONDS).untilAsserted(
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
    assertEquals(2, schedulerListener.getJobsAddedCount());
    assertEquals(2, schedulerListener.getJobsScheduledCount());
    assertEquals(1, schedulerListener.getJobsDeletedCount());
  }

  @Test
  void testScheduleForDisabledConfig() throws SchedulerException {
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, false);
    quartzExportJobScheduler.scheduleExportJob(config);

    assertFalse(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(0, schedulerListener.getJobsScheduledCount());
  }

  @Test
  void testScheduleJobWithMultipleTriggers() throws SchedulerException {
    int jobTriggersCount = 3;
    quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler, new TestTriggerConverter(jobTriggersCount),
      new TestJobDetailConverter(), new TestJobKeyResolver());
    TestingJobListener jobListener = registerTestingJobListener();
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, true);
    quartzExportJobScheduler.scheduleExportJob(config);

    assertTrue(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(1, schedulerListener.getJobsAddedCount());
    assertEquals(jobTriggersCount, schedulerListener.getJobsScheduledCount());

    await().pollDelay(1, TimeUnit.SECONDS).timeout(200, TimeUnit.SECONDS).untilAsserted(
      () -> assertEquals(jobTriggersCount, jobListener.getJobsExecutedCount()));
  }

  @Test
  void testUnscheduleJobWithMultipleTriggers() throws SchedulerException {
    int jobTriggersCount = 7;
    quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler, new TestTriggerConverter(jobTriggersCount),
      new TestJobDetailConverter(), new TestJobKeyResolver());
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, true);
    quartzExportJobScheduler.scheduleExportJob(config);

    // scheduleExportJob with disabled scheduling for existing id should result in job unscheduling
    ExportConfig disabledConfig = buildExportConfig(SCHEDULE_ID, false);
    quartzExportJobScheduler.scheduleExportJob(disabledConfig);

    assertFalse(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(1, schedulerListener.getJobsAddedCount());
    assertEquals(jobTriggersCount, schedulerListener.getJobsScheduledCount());
    assertEquals(1, schedulerListener.getJobsDeletedCount());
  }

  @Test
  void testRescheduleJobWithMultipleTriggers() throws SchedulerException {
    int jobTriggersInitialCount = 2;
    int jobTriggersRescheduleCount = 5;
    TestTriggerConverter testTriggerConverter = new TestTriggerConverter(jobTriggersInitialCount);
    quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler, testTriggerConverter,
      new TestJobDetailConverter(), new TestJobKeyResolver());
    TestingSchedulerListener schedulerListener = registerTestingSchedulerListener();

    ExportConfig config = buildExportConfig(SCHEDULE_ID, true);
    quartzExportJobScheduler.scheduleExportJob(config);

    // scheduleExportJob with enabled scheduling for existing id should result in job rescheduling
    ExportConfig reschedulingConfig = buildExportConfig(SCHEDULE_ID, true);
    testTriggerConverter.setTriggerAmount(jobTriggersRescheduleCount);
    quartzExportJobScheduler.scheduleExportJob(reschedulingConfig);

    assertTrue(scheduler.checkExists(JobKey.jobKey(SCHEDULE_ID)));
    assertEquals(2, schedulerListener.getJobsAddedCount());
    assertEquals(jobTriggersInitialCount + jobTriggersRescheduleCount, schedulerListener.getJobsScheduledCount());
    assertEquals(1, schedulerListener.getJobsDeletedCount());
  }

  @Test
  void testScheduleNullExportConfig() throws SchedulerException {
    quartzExportJobScheduler.scheduleExportJob(null);
    assertThat(scheduler.getJobKeys(GroupMatcher.anyJobGroup()), is(Collections.emptySet()));
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

  @AllArgsConstructor
  @NoArgsConstructor
  private static class TestTriggerConverter implements Converter<ExportConfig, ExportTrigger> {

    @Setter
    private int triggerAmount = 1;

    @Override
    public ExportTrigger convert(ExportConfig exportConfig) {
      Set<Trigger> triggers = new HashSet<>();
      for (int i = 0; i < triggerAmount; i++) {
        triggers.add(TriggerBuilder
          .newTrigger()
          .withIdentity(UUID.randomUUID().toString())
          .withSchedule(SimpleScheduleBuilder
            .simpleSchedule()
            .withRepeatCount(0))
          .startAt(Date.from(Instant.now().plus(2, ChronoUnit.SECONDS)))
          .build());
      }
      return new ExportTrigger(exportConfig.getSchedulePeriod() == SchedulePeriodEnum.NONE, triggers);
    }
  }
  private static class TestJobDetailConverter implements ExportConfigToJobDetailConverter {
    @Override
    public JobDetail convert(ExportConfig exportConfig, JobKey jobKey) {
      return JobBuilder.newJob(DraftJob.class).withIdentity(jobKey).build();
    }
  }

  private static class TestJobKeyResolver implements JobKeyResolver {
    @Override
    public JobKey resolve(ExportConfig exportConfig) {
      return JobKey.jobKey(exportConfig.getId());
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
    private final AtomicInteger jobsScheduledCount = new AtomicInteger();

    @Override
    public void jobDeleted(JobKey jobKey) {
      jobsDeletedCount.incrementAndGet();
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

    public int getJobsScheduledCount() {
      return jobsScheduledCount.get();
    }
  }
}
