package org.folio.des.scheduling;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.client.ExportWorkerClient;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.ExportConfig.WeekDaysEnum;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Metadata;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.security.AuthService;
import org.folio.des.security.SecurityManagerService;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.config.impl.ExportConfigServiceResolver;
import org.folio.des.service.impl.JobServiceImpl;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {ExportTrigger.class})
class ExportTriggerTest {

  private final int A_BIT_MORE_THAN_1_MINUTE = 65_000;

  @Autowired
  private ExportTrigger trigger;
  @MockBean
  private ExportConfigService bursarExportConfigService;
  @MockBean
  private FolioModuleMetadata folioModuleMetadata;
  @MockBean
  private AuthService authService;
  @MockBean
  private SecurityManagerService securityManagerService;
  @MockBean
  private ExportConfigValidatorResolver exportConfigValidatorResolver;
  @MockBean
  private JobCommandBuilderResolver jobCommandBuilderResolver;
  @MockBean
  private KafkaService kafka;
  @MockBean
  private ConfigurationClient client;
  @MockBean
  private JobServiceImpl jobService;
  @MockBean
  private ExportConfigServiceResolver exportConfigServiceResolver;
  @MockBean
  private DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  @MockBean
  private ObjectMapper objectMapper;
  @MockBean
  private ExportWorkerClient exportWorkerClient;


  @Test
  @DisplayName("No configuration for scheduling")
  void noConfig() {
    trigger.setConfig(null);

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
  }

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    trigger.setConfig(null);

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
  }

  @Test
  @DisplayName("No jobs are scheduled")
  void noneScheduled() {
    ExportConfig config = new ExportConfig();
    config.setScheduleFrequency(1);
    config.setSchedulePeriod(SchedulePeriodEnum.NONE);
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    trigger.setConfig(config);

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
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

    final Instant now = Instant.now();
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());
    Calendar nowPlusOneHour = Calendar.getInstance();
    nowPlusOneHour.setTimeInMillis(now.toEpochMilli());
    nowPlusOneHour.add(Calendar.HOUR, 1);
    assertTrue(DateUtils.truncatedEquals(nowPlusOneHour.getTime(), Date.from(instant), Calendar.HOUR));
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

    final Instant now = DateUtils.addHours(new Date(), scheduleFrequency).toInstant();
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext(now, now, now));

    assertNotNull(instant);
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

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNotNull(instant);
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

    final Instant now = Instant.now();
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext(now, now, now));

    assertNotNull(instant);
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

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNotNull(instant);
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


    final Instant now = Instant.now();
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext(now, now, now));

    assertNotNull(instant);
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

    Instant now = Instant.ofEpochMilli(cal.getTimeInMillis());
    SimpleTriggerContext triggerContext = new SimpleTriggerContext(now, now, now);
    Instant instant = trigger.nextExecution(triggerContext);

    Calendar actCal = Calendar.getInstance();
    actCal.setTimeInMillis(instant.toEpochMilli());
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

    Instant now = Instant.ofEpochMilli(cal.getTimeInMillis());
    SimpleTriggerContext triggerContext = new SimpleTriggerContext(now, now, now);
    Instant instant = trigger.nextExecution(triggerContext);

    Calendar actCal = Calendar.getInstance();
    actCal.setTimeInMillis(instant.toEpochMilli());
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

    Instant now = Instant.ofEpochMilli(cal.getTimeInMillis());
    SimpleTriggerContext triggerContext = new SimpleTriggerContext(now, now, now);
    Instant instant = trigger.nextExecution(triggerContext);

    Calendar actCal = Calendar.getInstance();
    actCal.setTimeInMillis(instant.toEpochMilli());
    int actLastHour = actCal.get(Calendar.HOUR_OF_DAY);
    assertEquals(adjustExpected(currHour + frequency), actLastHour);
  }

  @Test
  void disableScheduleForDeletedIntegration() {

    var repository = mock(JobDataExportRepository.class);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    var folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    var jobExecutionService = new JobExecutionService(kafka, exportConfigValidatorResolver, jobCommandBuilderResolver, defaultModelConfigToExportConfigConverter, client, objectMapper);
    var jobService = new JobServiceImpl(exportWorkerClient, jobExecutionService, repository, folioExecutionContext, null, null, client);
    var config = new ExportConfig();
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setExportConfigId(UUID.randomUUID());
    vendorEdiOrdersExportConfig.setConfigName("Test");
    exportTypeSpecificParameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    config.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    org.folio.des.domain.dto.Job jobDto = new org.folio.des.domain.dto.Job();
    jobDto.setMetadata(new Metadata());
    jobDto.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    jobDto.setId(UUID.randomUUID());
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    EdiSchedule ediSchedule = new EdiSchedule();
    ediSchedule.setScheduleParameters(scheduleParameters);
    jobDto.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().setEdiSchedule(ediSchedule);

    when(client.getConfigById(any())).thenThrow(new NotFoundException("Not Found"));

    assertThrows(NotFoundException.class, () -> jobService.upsertAndSendToKafka(jobDto, true));
    assertEquals(ScheduleParameters.SchedulePeriodEnum.NONE, jobDto.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig()
      .getEdiSchedule().getScheduleParameters().getSchedulePeriod());
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
