package org.folio.des.scheduling.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.ExportTrigger;
import org.folio.des.scheduling.acquisition.AcqBaseExportTaskTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.scheduling.TriggerContext;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.SimpleTriggerContext;

class AcqBaseExportTaskTriggerTest {

  public static final String ASIA_SHANGHAI_ZONE = "Asia/Shanghai";

  @Test
  @DisplayName("No configuratio`n for scheduling")
  void noConfig() {
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(null);
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("No jobs are scheduled")
  void noneScheduled() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(1);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
    scheduleParameters.setScheduleTime("12:00:00");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is set")
  void hourlyScheduleIfScheduledTimeIsSet() {
    int expDiffHours = 3;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR);
    scheduleParameters.setScheduleTime("11:12:13");
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);

    triggerContext.update(actDate, actDate, actDate);
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.getTime() - actDate.getTime());
    long diff = TimeUnit.HOURS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    Instant instant = Instant.ofEpochMilli(actDateHourLate.getTime());
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String lastDateStr = zonedDateTime.toString();
    assertTrue(lastDateStr.contains("14:12:13+08:00[Asia/Shanghai]"));
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is not set")
  void hourlyScheduleIfScheduledTimeIsNotSet() {
    int expDiffHours = 3;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR);
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);

    triggerContext.update(actDate, actDate, actDate);
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.getTime() - actDate.getTime());
    long diff = TimeUnit.HOURS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    ZonedDateTime nowDateTime = Instant.now().atZone(ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.DAYS);
    Instant lastInstant = Instant.ofEpochMilli(actDateHourLate.getTime()).truncatedTo(ChronoUnit.DAYS);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(lastInstant, ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.DAYS);
    assertTrue(nowDateTime.isEqual(zonedDateTime));
  }

  @Test
  @DisplayName("Should increase Days and provided time should be changed")
  void dailySchedule() {
    int expDiffHours = 3;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleTime("11:12:13");
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.DAY);
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);

    triggerContext.update(actDate, actDate, actDate);
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.getTime() - actDate.getTime());
    long diff = TimeUnit.DAYS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    ZonedDateTime nowDateTime = Instant.now().atZone(ZoneId.of(ASIA_SHANGHAI_ZONE)).plusDays(expDiffHours).truncatedTo(ChronoUnit.DAYS);
    Instant lastInstant = Instant.ofEpochMilli(actDateHourLate.getTime()).truncatedTo(ChronoUnit.SECONDS);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(lastInstant, ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.DAYS);
    assertTrue(nowDateTime.isEqual(zonedDateTime));
    //Time should not be changed
    Instant firstInstant = Instant.ofEpochMilli(actDateHourLate.getTime()).truncatedTo(ChronoUnit.SECONDS);
    ZonedDateTime firstDateTime = ZonedDateTime.ofInstant(firstInstant, ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.SECONDS);
    assertTrue(firstDateTime.toString().contains("11:12:13+08:00[Asia/Shanghai]"));
    ZonedDateTime lastDateTime = ZonedDateTime.ofInstant(lastInstant, ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.SECONDS);
    assertTrue(lastDateTime.toString().contains("11:12:13+08:00[Asia/Shanghai]"));
  }

  @Test
  @DisplayName("Weekly job scheduled")
  void weeklySchedule() {
    int expDiffHours = 3;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleTime("11:12:13");
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    scheduleParameters.setWeekDays(List.of(ScheduleParameters.WeekDaysEnum.MONDAY, ScheduleParameters.WeekDaysEnum.SATURDAY));
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);

    triggerContext.update(actDate, actDate, actDate);
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.getTime() - actDate.getTime());
    long diff = TimeUnit.DAYS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    ZonedDateTime nowDateTime = Instant.now().atZone(ZoneId.of(ASIA_SHANGHAI_ZONE)).plusDays(expDiffHours).truncatedTo(ChronoUnit.DAYS);
    Instant lastInstant = Instant.ofEpochMilli(actDateHourLate.getTime()).truncatedTo(ChronoUnit.SECONDS);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(lastInstant, ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.DAYS);
    assertTrue(nowDateTime.isEqual(zonedDateTime));
    //Time should not be changed
    Instant firstInstant = Instant.ofEpochMilli(actDateHourLate.getTime()).truncatedTo(ChronoUnit.SECONDS);
    ZonedDateTime firstDateTime = ZonedDateTime.ofInstant(firstInstant, ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.SECONDS);
    assertTrue(firstDateTime.toString().contains("11:12:13+08:00[Asia/Shanghai]"));
    ZonedDateTime lastDateTime = ZonedDateTime.ofInstant(lastInstant, ZoneId.of(ASIA_SHANGHAI_ZONE)).truncatedTo(ChronoUnit.SECONDS);
    assertTrue(lastDateTime.toString().contains("11:12:13+08:00[Asia/Shanghai]"));
  }
//
//  @Test
//  @DisplayName("Weekly job scheduled for specific days")
//  void weeklyScheduleWithWeekDays() {
//    ExportConfig exportConfig = new ExportConfig();
//    exportConfig.setId(UUID.randomUUID().toString());
//    exportConfig.setScheduleFrequency(1);
//    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.DAY);
//    exportConfig.setWeekDays(List.of(ExportConfig.WeekDaysEnum.FRIDAY));
//    exportConfig.setScheduleTime("12:00:00.000Z");
//    ExportTrigger exportTrigger = new ExportTrigger();
//    exportTrigger.setConfig(exportConfig);
//    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(exportTrigger);
//
//
//    final Date now = new Date();
//    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));
//
//    assertNotNull(date);
//  }


}
