package org.folio.des.scheduling;

import org.folio.de.entity.Job;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.security.AuthService;
import org.folio.des.security.SecurityManagerService;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.des.service.impl.JobServiceImpl;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.ExportConfig.WeekDaysEnum;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(classes = {ExportTrigger.class})
class ExportTriggerTest {

  private final int A_BIT_MORE_THAN_1_MINUTE = 65_000;

  @Autowired private ExportTrigger trigger;
  @MockBean private ExportConfigService bursarExportConfigService;
  @MockBean private FolioModuleMetadata folioModuleMetadata;
  @MockBean private AuthService authService;
  @MockBean private SecurityManagerService securityManagerService;
  @MockBean private ExportConfigValidatorResolver exportConfigValidatorResolver;
  @MockBean private JobCommandBuilderResolver jobCommandBuilderResolver;
  @MockBean private KafkaService kafka;
  @MockBean private ExportTypeBasedConfigManager exportTypeBasedConfigManager;

  @Test
  @DisplayName("No configuration for scheduling")
  void noConfig() {
    trigger.setConfig(null);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    trigger.setConfig(null);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("No jobs are scheduled")
  void noneScheduled() {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.NONE);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    trigger.setConfig(config);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Hourly job scheduled")
  void hourlySchedule() {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    config.setScheduleTime("15:06:00.000Z");
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    trigger.setConfig(config);

    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());
    Calendar nowPlusOneHour = Calendar.getInstance();
    nowPlusOneHour.setTime(now);
    nowPlusOneHour.add(Calendar.HOUR, 1);
    assertTrue(DateUtils.truncatedEquals(nowPlusOneHour.getTime(), date, Calendar.HOUR));
  }

  @Test
  @DisplayName("Hourly job scheduled for existing context")
  void hourlyScheduleExisting() {
    ExportConfig config = new ExportConfig();
    final int scheduleFrequency = 1;
    config.setScheduleFrequency(scheduleFrequency);
    config.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    config.setScheduleTime("15:06:00.000Z");
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    trigger.setConfig(config);

      final Date now =  DateUtils.addHours(new Date(), scheduleFrequency);
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));

    assertNotNull(date);
  }

  @Test
  @DisplayName("Daily job scheduled")
  void dailySchedule() {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.DAY);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    config.setScheduleTime("12:00:00.000Z");
    trigger.setConfig(config);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNotNull(date);
  }

  @Test
  @DisplayName("Daily job scheduled for existing context")
  void dailyScheduleExisting() {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.DAY);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    config.setScheduleTime("12:00:00.000Z");
    trigger.setConfig(config);

    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));

    assertNotNull(date);
  }

  @Test
  @DisplayName("Weekly job scheduled")
  void weeklySchedule() {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.WEEK);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    config.setScheduleTime("12:00:00.000Z");
    trigger.setConfig(config);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNotNull(date);
  }

  @DisplayName("Weekly job scheduled for specific days")
  void weeklyScheduleWithWeekDays() {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.WEEK);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    config.setScheduleTime("12:00:00.000Z");
    config.setWeekDays(List.of(WeekDaysEnum.FRIDAY));
    trigger.setConfig(config);


    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));

    assertNotNull(date);
  }

  @DisplayName("Hour job scheduled for specific hour, when last time behind current time")
  @ParameterizedTest
  @CsvSource({
    "1, 3",
    "2, 3",
    "4, 3"
  })
  void hourlyScheduleWithHours(int frequency, int addHours) {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    config.setScheduleTime("12:00:00.000Z");
    config.setScheduleFrequency(frequency);
    trigger.setConfig(config);

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.HOUR, -addHours);
    int currHour = cal.get(Calendar.HOUR_OF_DAY);

    Date now = cal.getTime();
    SimpleTriggerContext triggerContext = new SimpleTriggerContext(now, now, now);
    Date date = trigger.nextExecutionTime(triggerContext);

    Calendar actCal = Calendar.getInstance();
    actCal.setTime(date);
    int actLastHour = actCal.get(Calendar.HOUR_OF_DAY);
     assertEquals(adjustExpected(currHour + addHours + 1), actLastHour);
  }

  @DisplayName("Job scheduled for specific hour, when last time plus frequency equal to current time")
  @ParameterizedTest
  @CsvSource({
    "3, 3"
  })
  void hourlyScheduleWithHoursWhenLatTimePlusFrequencyEqualToCurrentTime(int frequency, int addHours) {
    ExportConfig config = new ExportConfig();
    config.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    config.setScheduleTime("12:00:00.000Z");
    config.setScheduleFrequency(frequency);
    trigger.setConfig(config);

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.HOUR, -addHours);
    int currHour = cal.get(Calendar.HOUR_OF_DAY);

    Date now = cal.getTime();
    SimpleTriggerContext triggerContext = new SimpleTriggerContext(now, now, now);
    Date date = trigger.nextExecutionTime(triggerContext);

    Calendar actCal = Calendar.getInstance();
    actCal.setTime(date);
    int actLastHour = actCal.get(Calendar.HOUR_OF_DAY);
    assertEquals(adjustExpected(currHour + addHours), actLastHour);
  }

  @DisplayName("Job scheduled for specific hour, When Frequency Higher Then Last Time More Then 2 Hours")
  @ParameterizedTest
  @CsvSource({
    "5, 3",
    "6, 3",
    "7, 3"
  })
  void hourlyScheduleWithHoursWhenFrequencyHigherThenLastTimeMoreThen2Hours(int frequency, int addHours) {
    ExportConfig config = new ExportConfig();
    config.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    config.setScheduleTime("12:00:00.000Z");
    config.setScheduleFrequency(frequency);
    trigger.setConfig(config);

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.HOUR, -addHours);
    int currHour = cal.get(Calendar.HOUR_OF_DAY);

    Date now = cal.getTime();
    SimpleTriggerContext triggerContext = new SimpleTriggerContext(now, now, now);
    Date date = trigger.nextExecutionTime(triggerContext);

    Calendar actCal = Calendar.getInstance();
    actCal.setTime(date);
    int actLastHour = actCal.get(Calendar.HOUR_OF_DAY);
    assertEquals(adjustExpected(currHour + frequency), actLastHour);
  }

  @Test
  void dailySchedulePlusOneMinuteWithExportScheduler() throws InterruptedException {
    var repository = mock(JobDataExportRepository.class);
    Job job = new Job();
    job.setId(UUID.randomUUID());
    when(repository.save(any(Job.class))).thenReturn(job);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    var folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    var jobExecutionService = new JobExecutionService(kafka, exportConfigValidatorResolver, jobCommandBuilderResolver);
    var jobService = new JobServiceImpl(jobExecutionService, repository, folioExecutionContext, null, null,exportTypeBasedConfigManager);
    var folioExecutionContextHelper =
      new FolioExecutionContextHelper(folioModuleMetadata, folioExecutionContext, authService, securityManagerService);
    folioExecutionContextHelper.registerTenant();
    var exportScheduler = new ExportScheduler(
      trigger, jobService, bursarExportConfigService, folioExecutionContextHelper, folioExecutionContext);
    var config = new ExportConfig();
    ExportTypeSpecificParameters a = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig v = new VendorEdiOrdersExportConfig();
    v.setExportConfigId(UUID.randomUUID());
    v.setConfigName("Test");
    a.setVendorEdiOrdersExportConfig(v);
    config.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.DAY);
    config.setExportTypeSpecificParameters(a);
    var now = LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(1);
    config.setScheduleTime(adjustHourOrMinute(now.getHour()) + ":" + adjustHourOrMinute(now.getMinute()) + ":00.000Z");
    config.setScheduleFrequency(1);
    config.setTenant("diku");
    trigger.setConfig(config);
    var scheduledTaskRegistrar = new ScheduledTaskRegistrar();
    exportScheduler.configureTasks(scheduledTaskRegistrar);
    exportScheduler.updateTasks(config);
    await().pollDelay(A_BIT_MORE_THAN_1_MINUTE, TimeUnit.MILLISECONDS)
      .timeout(A_BIT_MORE_THAN_1_MINUTE + 1, TimeUnit.MILLISECONDS).untilAsserted(() ->
      assertEquals(1, scheduledTaskRegistrar.getScheduledTasks().size()));
  }

  private int adjustExpected(int expected) {
    if (expected >= 24) {
      expected -= 24;
    }
    return expected;
  }

  private String adjustHourOrMinute(int val) {
    if (val < 10) {
      return "0" + val;
    }
    return String.valueOf(val);
  }
}
