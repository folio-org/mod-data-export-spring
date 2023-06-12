package org.folio.des.scheduling.quartz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.scheduling.BursarExportDeleteScheduler;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
  "folio.quartz.edifact.enabled=true", "folio.quartz.bursar.timeZone=Asia/Calcutta"})
class BursarExportDeleteJobSchedulerTest extends BaseTest {

  private static final String EXPORT_CONFIG_ID = UUID.randomUUID().toString();
  private static final String EXPORT_GROUP = TENANT + "_" + QuartzConstants.BURSAR_EXPORT_DELETE_GROUP_NAME;
  @Autowired
  BursarExportDeleteScheduler bursarExportDeleteScheduler;

  @Test
  void testBursarDeleteTrigger() throws SchedulerException {
    var testConfig = createConfig();

    bursarExportDeleteScheduler.scheduleBursarDeleteJob(testConfig);
    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());

    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator().next());
    assertEquals(1, triggers.size());
    assertEquals(EXPORT_GROUP, triggers.get(0).getKey().getGroup());
  }

  private ExportConfig createConfig() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(EXPORT_CONFIG_ID);
    exportConfig.setType(ExportType.BURSAR_FEES_FINES);
    exportConfig.setTenant(TENANT);
    return exportConfig;
  }
}
