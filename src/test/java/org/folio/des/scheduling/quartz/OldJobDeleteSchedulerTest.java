package org.folio.des.scheduling.quartz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.PostConstruct;

import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
class OldJobDeleteSchedulerTest extends BaseTest {
  private static final String EXPORT_GROUP = TENANT + "_" + QuartzConstants.EXPORT_DELETE_GROUP_NAME;

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
    var jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupContains(EXPORT_GROUP));
    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator().next());
    assertEquals(1, triggers.size());
    assertEquals(EXPORT_GROUP, triggers.get(0).getKey().getGroup());
  }

  @Test
  void testDeleteOldJobDeleteScheduler() throws SchedulerException {

    oldJobDeleteScheduler.scheduleOldJobDeletion(TENANT);
    oldJobDeleteScheduler.removeOldJobDeletionScheduler(TENANT);
    var jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupContains(EXPORT_GROUP));
    assertTrue(jobKeys.isEmpty());
  }
}
