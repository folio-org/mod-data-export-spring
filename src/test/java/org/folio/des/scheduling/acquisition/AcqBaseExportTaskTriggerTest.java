package org.folio.des.scheduling.acquisition;

import static org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum;
import static org.folio.des.domain.dto.ScheduleParameters.WeekDaysEnum;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.folio.des.domain.dto.ScheduleParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.scheduling.support.SimpleTriggerContext;

class AcqBaseExportTaskTriggerTest {

  public static final String ASIA_SHANGHAI_ZONE = "Asia/Shanghai";
  public static final String EUROPE_MONACO = "Europe/Monaco";

  @Test
  @DisplayName("No configuration for scheduling")
  void noConfig() {
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(null, null, true);
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
  }

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
  }

  @Test
  @DisplayName("No jobs are scheduled")
  void noneScheduled() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(1);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.NONE);
    scheduleParameters.setScheduleTime("12:00:00");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
  }

  @Test
  @DisplayName("Is disabled schedule")
  void isDisabledSchedule() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(1);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.NONE);
    scheduleParameters.setScheduleTime("12:00:00");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, false);

    assertTrue(trigger.isDisabledSchedule());
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is set")
  @Disabled
  void hourlyScheduleIfScheduledTimeIsSet() {
    int expDiffHours = 7;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    scheduleParameters.setScheduleTime("11:12:13");
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();

    final Instant actInstant = trigger.nextExecution(new SimpleTriggerContext());

    ZonedDateTime firstZonedDateTime = ZonedDateTime.ofInstant(actInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String firstDateStr = firstZonedDateTime.toString();
    assertTrue(firstDateStr.contains("11:12:13+08:00[Asia/Shanghai]"));
    int dayOfMonth = firstZonedDateTime.getDayOfMonth();

    triggerContext.update(actInstant, actInstant, actInstant);
    final Instant actDateHourLate = trigger.nextExecution(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.toEpochMilli() - actInstant.toEpochMilli());
    long diff = TimeUnit.HOURS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(actDateHourLate, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String lastDateStr = zonedDateTime.toString();
    assertTrue(lastDateStr.contains(dayOfMonth+"T18:12:13+08:00[Asia/Shanghai]"));

    Instant actDateHourLateClone = Instant.ofEpochMilli(actDateHourLate.toEpochMilli());
    triggerContext.update(actDateHourLateClone, actDateHourLateClone,actDateHourLateClone);
    final Instant thirdDateHourLate = trigger.nextExecution(triggerContext);
    long thirdDiffInMilliseconds = Math.abs(thirdDateHourLate.toEpochMilli() - actDateHourLate.toEpochMilli());
    long thirdDiff = TimeUnit.HOURS.convert(thirdDiffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, thirdDiff);

    ZonedDateTime thirdZonedDateTime = ZonedDateTime.ofInstant(thirdDateHourLate, ZoneId.of(ASIA_SHANGHAI_ZONE));
    String thirdLastDateStr = thirdZonedDateTime.toString();
    dayOfMonth = dayOfMonth + 1;
    assertTrue(thirdLastDateStr.contains(dayOfMonth+"T01:12:13+08:00[Asia/Shanghai]"));
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is not set")
  void hourlyScheduleIfScheduledTimeIsNotSet() {
    int expDiffHours = 1;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    scheduleParameters.setTimeZone("UTC");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Instant actDate = trigger.nextExecution(triggerContext);

    triggerContext.update(actDate, actDate, actDate);
    final Instant actDateHourLate = trigger.nextExecution(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.toEpochMilli() - actDate.toEpochMilli());
    long diff = TimeUnit.HOURS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    ZonedDateTime expDateTime =
      Instant.now().atZone(ZoneId.of("UTC")).plusHours(expDiffHours).truncatedTo(ChronoUnit.DAYS);
    Instant lastInstant = Instant.ofEpochMilli(actDate.toEpochMilli()).truncatedTo(ChronoUnit.DAYS);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(lastInstant, ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
    assertTrue(expDateTime.isEqual(zonedDateTime));
  }

  @Test
  @DisplayName("Should increase Days and provided time should be changed")
  void dailySchedule() {
    int expDiffDays = 3;
    ZoneId zoneId = ZoneId.of("Asia/Shanghai");
    ZonedDateTime nowTime = getNowTime(zoneId);
    ZonedDateTime scheduledDateTime = nowTime.plusHours(1);
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleTime(scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
    scheduleParameters.setScheduleFrequency(expDiffDays);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.DAY);
    scheduleParameters.setTimeZone(ASIA_SHANGHAI_ZONE);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Instant actDate = trigger.nextExecution(triggerContext);

    ZonedDateTime firstZonedDateTime = ZonedDateTime.ofInstant(actDate, ZoneId.of(ASIA_SHANGHAI_ZONE));
    assertEquals(scheduledDateTime.getDayOfMonth(), firstZonedDateTime.getDayOfMonth());
    assertEquals(scheduledDateTime.getHour(), firstZonedDateTime.getHour());
    assertEquals(scheduledDateTime.getMinute(), firstZonedDateTime.getMinute());

    //Second try
    triggerContext.update(actDate, actDate, actDate);
    final Instant actDateDayLate = trigger.nextExecution(triggerContext);
    long diffInMilliseconds = Math.abs(actDateDayLate.toEpochMilli() - actDate.toEpochMilli());
    long diff = TimeUnit.DAYS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffDays, diff);

    ZonedDateTime secondZonedDateTime = ZonedDateTime.ofInstant(actDateDayLate, zoneId);
    assertEquals(scheduledDateTime.plusDays(expDiffDays).getDayOfMonth(), secondZonedDateTime.getDayOfMonth());
    assertEquals(scheduledDateTime.getHour(), secondZonedDateTime.getHour());
    assertEquals(scheduledDateTime.getMinute(), secondZonedDateTime.getMinute());

    //Third try
    triggerContext.update(actDateDayLate, actDateDayLate, actDateDayLate);
    final Instant thirdDateDayLate = trigger.nextExecution(triggerContext);
    long thirdDiffInMilliseconds = Math.abs(thirdDateDayLate.toEpochMilli() - actDateDayLate.toEpochMilli());
    long thirdDiff = TimeUnit.DAYS.convert(thirdDiffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffDays, thirdDiff);

    ZonedDateTime thirdZonedDateTime = ZonedDateTime.ofInstant(thirdDateDayLate, ZoneId.of(ASIA_SHANGHAI_ZONE));
    assertEquals(scheduledDateTime.plusDays(expDiffDays * 2).getDayOfMonth(), thirdZonedDateTime.getDayOfMonth());
    assertEquals(scheduledDateTime.getHour(), thirdZonedDateTime.getHour());
    assertEquals(scheduledDateTime.getMinute(), thirdZonedDateTime.getMinute());
  }

  @Test
  @DisplayName("Daily run should be executed tomorrow in case when scheduled datetime less than current datetime")
  public void dailyScheduleWhenScheduledDateLessThanCurrentDate() {
    //Given
    ZonedDateTime nowTime = getNowTime();
    ZonedDateTime scheduledDateTime = nowTime.minusHours(1);
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setScheduleTime(scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
    scheduleParameters.setScheduleFrequency(1);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.DAY);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);

    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(scheduledDateTime.plusDays(1).getDayOfMonth(), actDateTime.getDayOfMonth());
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
    assertEquals(scheduledDateTime.getMinute(), actDateTime.getMinute());
  }

  @DisplayName("Weekly job scheduled for specific hour, when last time behind current time")
  @ParameterizedTest
  @CsvSource({
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY"
  })
  public void weeklyTestForEachDay(String weekDay) {
    //Given
    ScheduleParameters scheduleParameters = getScheduleParameters(List.of(weekDay), null);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);

    //When
    Instant now = Instant.now();
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(now, now, now);
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(actDateTime.getDayOfWeek().toString(), weekDay);
  }

  /**
   * For example, now is Monday 15:00, user selects to schedule on Monday at 14:00 - so scheduling should be triggered after a week
   */
  @Test
  public void weeklyTestForTheSameDayAndTimeIsInPast() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.minusHours(1);
    ScheduleParameters scheduleParameters = getScheduleParameters(List.of(scheduledDateTime.getDayOfWeek().toString()),
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(Instant.now(), null, Instant.now());
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(scheduledDateTime.getDayOfWeek(), actDateTime.getDayOfWeek());
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
  }

  /**
   * For example, now is Monday 15:00, user selects to schedule on Monday at 16:00 - so scheduling should be triggered at the same day after a hour
   */
  @Test
  public void weeklyTestForTheSameDayAndTimeIsInFuture() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.plusHours(1);
    ScheduleParameters scheduleParameters = getScheduleParameters(List.of(scheduledDateTime.getDayOfWeek().toString()),
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(Instant.now(), null, Instant.now());
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(scheduledDateTime.getDayOfMonth(), actDateTime.getDayOfMonth());
    assertEquals(scheduledDateTime.getDayOfWeek(), actDateTime.getDayOfWeek());
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
  }

  /**
   * For example, now is Wednesday 15:00, user selects to schedule on Monday at 15:00 - so scheduling should be triggered after 5 days
   */
  @Test
  public void weeklyTestForDayOfWeekInPast() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.minusDays(2);
    ScheduleParameters scheduleParameters = getScheduleParameters(List.of(scheduledDateTime.getDayOfWeek().toString()),
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(Instant.now(), null, Instant.now());
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(scheduledDateTime.getDayOfWeek(), actDateTime.getDayOfWeek());
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
  }

  /**
   * For example, now is Tuesday 10:00, and user selects Monday, Tuesday and Wednesday at 08:00 - so scheduling should be run on Wednesday
   */
  @Test
  public void weeklyTestForMultipleDays() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.minusHours(1);
    DayOfWeek currentDay = scheduledDateTime.getDayOfWeek();
    DayOfWeek dayBefore = getNowTime().minusDays(1).getDayOfWeek();
    DayOfWeek dayAfter = getNowTime().plusDays(1).getDayOfWeek();
    ScheduleParameters scheduleParameters = getScheduleParameters(List.of(currentDay.name(), dayBefore.name(), dayAfter.name()),
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(Instant.now(), null, Instant.now());
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(dayAfter, actDateTime.getDayOfWeek());
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
  }

  /**
   * For example, if user selects scheduled frequency - 2, we need to shift scheduling day for 2 weeks
   */
  @Test
  public void weeklyTestScheduleOnceAtTwoWeek() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.minusHours(1);
    ScheduleParameters scheduleParameters = getScheduleParameters(List.of(scheduledDateTime.getDayOfWeek().toString()),
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
    scheduleParameters.setScheduleFrequency(2);

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(Instant.now(), null, Instant.now());
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertNotEquals(scheduledDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), actDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    assertEquals(scheduledDateTime.getDayOfWeek(), actDateTime.getDayOfWeek());
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
  }

  @Test
  public void weeklyTestScheduleOnceWithMultipleChosenDaysAndFrequencyWhenDayFromNextWeekChosen() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.minusHours(1);
    DayOfWeek currentDay = scheduledDateTime.getDayOfWeek();
    DayOfWeek dayBefore = getNowTime().minusDays(1).getDayOfWeek();
    ScheduleParameters scheduleParameters = getScheduleParameters(List.of(currentDay.name(), dayBefore.name()),
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
    scheduleParameters.scheduleFrequency(2);

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(Instant.now(), null, Instant.now());
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
    assertEquals(dayBefore, actDateTime.getDayOfWeek());
  }

  @Test
  public void weeklyTestWhenAllDaysChosen() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.minusHours(1);
    DayOfWeek[] values = DayOfWeek.values();
    List<String> allDaysOfWeek = Arrays.stream(values).map(DayOfWeek::name).toList();
    ScheduleParameters scheduleParameters = getScheduleParameters(allDaysOfWeek,
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(Instant.now(), null, Instant.now());
    Instant actDate = trigger.nextExecution(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(scheduledDateTime.plusDays(1).getDayOfWeek(), actDateTime.getDayOfWeek());
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
  }

  private ScheduleParameters getScheduleParameters(List<String> weekDays, String scheduledTime) {
    ScheduleParameters params = new ScheduleParameters();
    params.setId(UUID.randomUUID());
    params.setScheduleFrequency(1);
    params.setSchedulePeriod(SchedulePeriodEnum.WEEK);
    params.setWeekDays(weekDays.stream().map(WeekDaysEnum::fromValue).toList());
    params.setTimeZone("UTC");
    params.setScheduleTime(scheduledTime);
    return params;
  }

  private ZonedDateTime getNowTime() {
    return getNowTime(ZoneId.of("UTC"));
  }

  private ZonedDateTime getNowTime(ZoneId zoneId) {
    Calendar scheduledCal = Calendar.getInstance();
    Date scheduledDate = scheduledCal.getTime();
    Instant scheduledInstant = Instant.ofEpochMilli(scheduledDate.getTime());
    return ZonedDateTime.ofInstant(scheduledInstant, zoneId);
  }

  private ZonedDateTime getActualTime(Instant actDate) {
    return ZonedDateTime.ofInstant(actDate, ZoneId.of("UTC"));
  }

  @DisplayName("Hour job scheduled for specific hour, when last time behind current time")
  @ParameterizedTest
  @CsvSource({
    "1, 3, 1",
    "2, 3, 1",
    "4, 3, 1",
    "5, 3, 2",
    "6, 3, 3",
    "7, 3, 4",
    "3, 0, 3",
    "3, 3, 1"
  })
  void hourlyScheduleWithHours(int frequency, int addHours, int expDiffHours) {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(frequency);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    scheduleParameters.setTimeZone("UTC");

    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    //When
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.HOUR, -addHours);
    Instant now = Instant.ofEpochMilli(cal.getTimeInMillis());
    ZonedDateTime nowZonedDateTime = ZonedDateTime.ofInstant(now, ZoneId.of("UTC"));
    int nowHour = nowZonedDateTime.getHour();

    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(now, now, now);
    Instant act = trigger.nextExecution(triggerContext);

    ZonedDateTime actZonedDateTime = ZonedDateTime.ofInstant(act, ZoneId.of("UTC"));
    int actHour = actZonedDateTime.getHour();
    assertEquals((nowHour + expDiffHours + addHours) % 24, actHour);
  }

  @DisplayName("Hour job scheduled for specific hour, when last time behind current time")
  @ParameterizedTest
  @CsvSource({
    "1, 3, 1",
    "2, 3, 1",
    "4, 3, 1",
    "5, 3, 2",
    "6, 3, 3",
    "7, 3, 4",
    "3, 0, 3",
    "3, 3, 1"
  })
  void hourlyScheduleWithHoursAndLastJobStartItCanHappenAfterModuleRestart(int frequency, int addHours, int expDiffHours) {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(frequency);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    scheduleParameters.setTimeZone("UTC");

    Calendar calLastJob = Calendar.getInstance();
    calLastJob.add(Calendar.HOUR, -addHours);
    Date lastJobTime = calLastJob.getTime();
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, lastJobTime, true);
    //When
    Instant nowInstant = Instant.ofEpochMilli(lastJobTime.getTime());
    ZonedDateTime lastJobZonedDateTime = ZonedDateTime.ofInstant(nowInstant, ZoneId.of("UTC"));
    int nowHour = lastJobZonedDateTime.getHour();

    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update((Instant) null, null, null);
    Instant actInstant = trigger.nextExecution(triggerContext);

    ZonedDateTime actZonedDateTime = ZonedDateTime.ofInstant(actInstant, ZoneId.of("UTC"));
    int actHour = actZonedDateTime.getHour();
    assertEquals((nowHour + expDiffHours + addHours) % 24, actHour);
  }
}
