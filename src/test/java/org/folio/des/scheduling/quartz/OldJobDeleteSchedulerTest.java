package org.folio.des.scheduling.quartz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;



import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Log4j2
@SpringBootTest(properties = { "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}" })
class OldJobDeleteSchedulerTest extends BaseTest {
  private static final String EXPORT_DELETE_GROUP = TENANT + "_" + QuartzConstants.OLD_JOB_DELETE_GROUP_NAME;

  @Autowired
  private Scheduler scheduler;
  private OldJobDeleteScheduler oldJobDeleteScheduler;

  @PostConstruct
  public void setUpOldJobDeleteScheduler() {
    oldJobDeleteScheduler = new OldJobDeleteScheduler(scheduler);
  }

  @Test
  void testOldJobDeleteTrigger() throws SchedulerException {

    oldJobDeleteScheduler.scheduleOldJobDeletion(TENANT);
    var jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupContains(EXPORT_DELETE_GROUP));
    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator()
      .next());
    assertEquals(1, triggers.size());
    assertEquals(EXPORT_DELETE_GROUP, triggers.get(0)
      .getKey()
      .getGroup());
  }

  @Test
  void testDeleteOldJobDeleteScheduler() throws SchedulerException {

    oldJobDeleteScheduler.scheduleOldJobDeletion(TENANT);
    oldJobDeleteScheduler.removeJobs(TENANT);
    await().pollDelay(1, TimeUnit.SECONDS)
      .timeout(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        var jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupContains(EXPORT_DELETE_GROUP));
        log.info("jobKeys: {}", jobKeys);
        assertTrue(jobKeys.isEmpty());
      });
  }
}
