package org.folio.des.scheduling.acquisition;

import static org.folio.des.domain.dto.ScheduleParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.folio.des.domain.dto.ScheduleParameters;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.SimpleTriggerContext;

class AcqBaseExportTaskTriggerTest {

  public static final String ASIA_SHANGHAI_ZONE = "Asia/Shanghai";

  @Test
  @DisplayName("No configuratio`n for scheduling")
  void noConfig() {
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(null, true);
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, true);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("No jobs are scheduled")
  void noneScheduled() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(1);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.NONE);
    scheduleParameters.setScheduleTime("12:00:00");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, true);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is set")
  void hourlyScheduleIfScheduledTimeIsSet() {
    int expDiffHours = 7;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    scheduleParameters.setScheduleTime("11:12:13");
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, true);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();

    final Date actDate = trigger.nextExecutionTime(triggerContext);

    Instant firstInstant = Instant.ofEpochMilli(actDate.getTime());
    ZonedDateTime firstZonedDateTime = ZonedDateTime.ofInstant(firstInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String firstDateStr = firstZonedDateTime.toString();
    assertTrue(firstDateStr.contains("11:12:13+08:00[Asia/Shanghai]"));
    int dayOfMonth = firstZonedDateTime.getDayOfMonth();

    triggerContext.update(actDate, actDate, actDate);
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.getTime() - actDate.getTime());
    long diff = TimeUnit.HOURS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    Instant instant = Instant.ofEpochMilli(actDateHourLate.getTime());
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String lastDateStr = zonedDateTime.toString();
    assertTrue(lastDateStr.contains(dayOfMonth+"T18:12:13+08:00[Asia/Shanghai]"));

    triggerContext.update((Date) actDateHourLate.clone(), (Date) actDateHourLate.clone(), (Date) actDateHourLate.clone());
    final Date thirdDateHourLate = trigger.nextExecutionTime(triggerContext);
    long thirdDiffInMilliseconds = Math.abs(thirdDateHourLate.getTime() - actDateHourLate.getTime());
    long thirdDiff = TimeUnit.HOURS.convert(thirdDiffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, thirdDiff);

    Instant thirdInstant = Instant.ofEpochMilli(thirdDateHourLate.getTime());
    ZonedDateTime thirdZonedDateTime = ZonedDateTime.ofInstant(thirdInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String thirdLastDateStr = thirdZonedDateTime.toString();
    dayOfMonth = dayOfMonth + 1;
    assertTrue(thirdLastDateStr.contains(dayOfMonth+"T01:12:13+08:00[Asia/Shanghai]"));
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is not set")
  void hourlyScheduleIfScheduledTimeIsNotSet() {
    int expDiffHours = 3;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, true);
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
    int expDiffDays = 3;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleTime("11:12:13");
    scheduleParameters.setScheduleFrequency(expDiffDays);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.DAY);
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, true);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);

    Instant firstInstant = Instant.ofEpochMilli(actDate.getTime());
    ZonedDateTime firstZonedDateTime = ZonedDateTime.ofInstant(firstInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String firstDateStr = firstZonedDateTime.toString();
    int dayOfMonth = firstZonedDateTime.getDayOfMonth();
    assertTrue(firstDateStr.contains(dayOfMonth+"T11:12:13+08:00[Asia/Shanghai]"));

    //Second try
    triggerContext.update(actDate, actDate, actDate);
    final Date actDateDayLate = trigger.nextExecutionTime(triggerContext);
    long diffInMilliseconds = Math.abs(actDateDayLate.getTime() - actDate.getTime());
    long diff = TimeUnit.DAYS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffDays, diff);

    Instant instant = Instant.ofEpochMilli(actDateDayLate.getTime());
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    dayOfMonth = dayOfMonth + expDiffDays;
    String lastDateStr = zonedDateTime.toString();
    assertTrue(lastDateStr.contains(dayOfMonth+"T11:12:13+08:00[Asia/Shanghai]"));
    //Third try
    triggerContext.update(actDateDayLate, actDateDayLate, actDateDayLate);
    final Date thirdDateDayLate = trigger.nextExecutionTime(triggerContext);
    long thirdDiffInMilliseconds = Math.abs(thirdDateDayLate.getTime() - actDateDayLate.getTime());
    long thirdDiff = TimeUnit.DAYS.convert(thirdDiffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffDays, thirdDiff);

    Instant thirdInstant = Instant.ofEpochMilli(thirdDateDayLate.getTime());
    ZonedDateTime thirdZonedDateTime = ZonedDateTime.ofInstant(thirdInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    dayOfMonth = zonedDateTime.getDayOfMonth() + expDiffDays;
    String thirdLastDateStr = thirdZonedDateTime.toString();
    assertTrue(thirdLastDateStr.contains(dayOfMonth+"T11:12:13+08:00[Asia/Shanghai]"));
  }

  @Test
  @DisplayName("Weekly job scheduled")
  void weeklySchedule() {
    int expDiffWeeks = 0;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleTime("11:12:13");
    scheduleParameters.setScheduleFrequency(expDiffWeeks);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.WEEK);
    scheduleParameters.setWeekDays(List.of(WeekDaysEnum.MONDAY, WeekDaysEnum.SATURDAY, WeekDaysEnum.SUNDAY));
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, true);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);

    Instant firstInstant = Instant.ofEpochMilli(actDate.getTime());
    ZonedDateTime firstZonedDateTime = ZonedDateTime.ofInstant(firstInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    DayOfWeek firstDayOfWeek = firstZonedDateTime.getDayOfWeek();
    assertEquals(WeekDaysEnum.MONDAY.getValue(), firstDayOfWeek.toString());
    final String EXP_TIME = "T11:12:13+08:00[Asia/Shanghai]";
    assertTrue(firstZonedDateTime.toString().contains(firstZonedDateTime.getDayOfMonth()+ EXP_TIME));

    //Second try
    triggerContext.update(actDate, actDate, actDate);
    final Date secondDateDayLate = trigger.nextExecutionTime(triggerContext);

    Instant instant = Instant.ofEpochMilli(secondDateDayLate.getTime());
    ZonedDateTime secondZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    DayOfWeek secondDayOfWeek = secondZonedDateTime.getDayOfWeek();
    assertEquals(WeekDaysEnum.SATURDAY.getValue(), secondDayOfWeek.toString());
    assertTrue(secondZonedDateTime.toString().contains(secondZonedDateTime.getDayOfMonth()+ EXP_TIME));
    //Third try
    triggerContext.update(secondDateDayLate, secondDateDayLate, secondDateDayLate);
    final Date thirdDateDayLate = trigger.nextExecutionTime(triggerContext);

    Instant thirdInstant = Instant.ofEpochMilli(thirdDateDayLate.getTime());
    ZonedDateTime thirdZonedDateTime = ZonedDateTime.ofInstant(thirdInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    DayOfWeek thirdDayOfWeek = thirdZonedDateTime.getDayOfWeek();
    assertEquals(WeekDaysEnum.SUNDAY.getValue(), thirdDayOfWeek.toString());
    assertTrue(thirdZonedDateTime.toString().contains(thirdZonedDateTime.getDayOfMonth()+ EXP_TIME));
    //Forth try
    triggerContext.update(thirdDateDayLate, thirdDateDayLate, thirdDateDayLate);
    final Date forthDateDayLate = trigger.nextExecutionTime(triggerContext);

    Instant forthInstant = Instant.ofEpochMilli(forthDateDayLate.getTime());
    ZonedDateTime forthZonedDateTime = ZonedDateTime.ofInstant(forthInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    DayOfWeek forthDayOfWeek = forthZonedDateTime.getDayOfWeek();
    assertEquals(WeekDaysEnum.MONDAY.getValue(), forthDayOfWeek.toString());
    assertTrue(forthZonedDateTime.toString().contains(forthZonedDateTime.getDayOfMonth() + EXP_TIME));
  }
}
