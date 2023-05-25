package org.folio.des.scheduling.quartz;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarJobDetailConverter;
import org.folio.des.scheduling.quartz.converter.bursar.ExportConfigToBursarTriggerConverter;
import org.folio.des.scheduling.quartz.job.bursar.BursarJobKeyResolver;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
  "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
  "folio.quartz.edifact.enabled=true","folio.quartz.bursar.timeZone=Asia/Calcutta"})
class BursarExportJobSchedulerTest extends BaseTest {

  private static final String EXPORT_CONFIG_ID = UUID.randomUUID().toString();
  public static final String SCHEDULE_TIME = "20:59:00.000Z";
  @Autowired
  ExportConfigToBursarTriggerConverter exportConfigToBursarTriggerConverter;

  @Autowired
  BursarJobKeyResolver bursarJobKeyResolver;

  @Autowired
  ExportConfigToBursarJobDetailConverter exportConfigToBursarJobDetailConverter;

  private QuartzExportJobScheduler quartzExportJobScheduler;

  @PostConstruct
  public void setUpBursar() {
    quartzExportJobScheduler = new QuartzExportJobScheduler(scheduler,
      exportConfigToBursarTriggerConverter,exportConfigToBursarJobDetailConverter,bursarJobKeyResolver);
  }

  @ParameterizedTest
  @EnumSource(value = ExportConfig.SchedulePeriodEnum.class, names = {"HOUR","DAY","WEEK"})
  void testBursarTrigger(ExportConfig.SchedulePeriodEnum schedulePeriodEnum) throws SchedulerException {
    var testConfig = createConfig(schedulePeriodEnum);

    quartzExportJobScheduler.scheduleExportJob(testConfig);
    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());

    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator().next());
    assertEquals(1, triggers.size());

  }

  @Test
  void testNoBursarTriggerIfPeriodIsNone() throws SchedulerException {
    var testConfig = createConfig(ExportConfig.SchedulePeriodEnum.NONE);

    quartzExportJobScheduler.scheduleExportJob(testConfig);
    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertTrue(jobKeys.isEmpty());
  }

  private ExportConfig createConfig(ExportConfig.SchedulePeriodEnum schedulePeriodEnum) {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(EXPORT_CONFIG_ID);
    exportConfig.setType(ExportType.BURSAR_FEES_FINES);
    exportConfig.setTenant(TENANT);
    exportConfig.setScheduleTime(SCHEDULE_TIME);
    exportConfig.setSchedulePeriod(schedulePeriodEnum);
    exportConfig.setScheduleFrequency(1);
    if(ExportConfig.SchedulePeriodEnum.WEEK.equals(schedulePeriodEnum)) {
      exportConfig.setWeekDays(List.of(ExportConfig.WeekDaysEnum.TUESDAY));
    }
    return exportConfig;
  }
}
