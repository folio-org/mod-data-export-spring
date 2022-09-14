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
import java.util.stream.Collectors;

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
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);

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
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
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
    int expDiffHours = 1;
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(expDiffHours);
    scheduleParameters.setSchedulePeriod(SchedulePeriodEnum.HOUR);
    scheduleParameters.setTimeZone("UTC");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);

    triggerContext.update(actDate, actDate, actDate);
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    long diffInMilliseconds = Math.abs(actDateHourLate.getTime() - actDate.getTime());
    long diff = TimeUnit.HOURS.convert(diffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffHours, diff);

    ZonedDateTime nowDateTime = Instant.now().atZone(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
    Instant lastInstant = Instant.ofEpochMilli(actDateHourLate.getTime()).truncatedTo(ChronoUnit.DAYS);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(lastInstant, ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
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
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
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
    ZonedDateTime secondZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    dayOfMonth = firstZonedDateTime.plusDays(expDiffDays).getDayOfMonth();
    String lastDateStr = secondZonedDateTime.toString();
    assertTrue(lastDateStr.contains(dayOfMonth+"T11:12:13+08:00[Asia/Shanghai]"));
    //Third try
    triggerContext.update(actDateDayLate, actDateDayLate, actDateDayLate);
    final Date thirdDateDayLate = trigger.nextExecutionTime(triggerContext);
    long thirdDiffInMilliseconds = Math.abs(thirdDateDayLate.getTime() - actDateDayLate.getTime());
    long thirdDiff = TimeUnit.DAYS.convert(thirdDiffInMilliseconds, TimeUnit.MILLISECONDS);
    assertEquals(expDiffDays, thirdDiff);

    Instant thirdInstant = Instant.ofEpochMilli(thirdDateDayLate.getTime());
    ZonedDateTime thirdZonedDateTime = ZonedDateTime.ofInstant(thirdInstant, ZoneId.of(ASIA_SHANGHAI_ZONE));
    dayOfMonth = secondZonedDateTime.plusDays(expDiffDays).getDayOfMonth();
    String thirdLastDateStr = thirdZonedDateTime.toString();
    assertTrue(thirdLastDateStr.contains(dayOfMonth+"T11:12:13+08:00[Asia/Shanghai]"));
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
    Date now = new Date();
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(now, now, now);
    Date actDate = trigger.nextExecutionTime(triggerContext);

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
    triggerContext.update(new Date(), null, new Date());
    Date actDate = trigger.nextExecutionTime(triggerContext);

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
    triggerContext.update(new Date(), null, new Date());
    Date actDate = trigger.nextExecutionTime(triggerContext);

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
    triggerContext.update(new Date(), null, new Date());
    Date actDate = trigger.nextExecutionTime(triggerContext);

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
    triggerContext.update(new Date(), null, new Date());
    Date actDate = trigger.nextExecutionTime(triggerContext);

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
    triggerContext.update(new Date(), null, new Date());
    Date actDate = trigger.nextExecutionTime(triggerContext);

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
    triggerContext.update(new Date(), null, new Date());
    Date actDate = trigger.nextExecutionTime(triggerContext);

    //Then
    ZonedDateTime actDateTime = getActualTime(actDate);
    assertEquals(scheduledDateTime.getHour(), actDateTime.getHour());
    assertEquals(dayBefore, actDateTime.getDayOfWeek());
    assertNotEquals(scheduledDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), actDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
  }

  @Test
  public void weeklyTestWhenAllDaysChosen() {
    //Given
    ZonedDateTime scheduledDateTime = getNowTime();
    scheduledDateTime = scheduledDateTime.minusHours(1);
    DayOfWeek[] values = DayOfWeek.values();
    List<String> allDaysOfWeek = Arrays.stream(values).map(DayOfWeek::name).collect(Collectors.toList());
    ScheduleParameters scheduleParameters = getScheduleParameters(allDaysOfWeek,
      scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));

    //When
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters, null, true);
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(new Date(), null, new Date());
    Date actDate = trigger.nextExecutionTime(triggerContext);

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
    params.setWeekDays(weekDays.stream().map(WeekDaysEnum::fromValue).collect(Collectors.toList()));
    params.setTimeZone("UTC");
    params.setScheduleTime(scheduledTime);
    return params;
  }

  private ZonedDateTime getNowTime() {
    Calendar scheduledCal = Calendar.getInstance();
    Date scheduledDate = scheduledCal.getTime();
    Instant scheduledInstant = Instant.ofEpochMilli(scheduledDate.getTime());
    return ZonedDateTime.ofInstant(scheduledInstant, ZoneId.of("UTC"));
  }

  private ZonedDateTime getActualTime(Date actDate) {
    Instant actInstant = Instant.ofEpochMilli(actDate.getTime());
    return ZonedDateTime.ofInstant(actInstant, ZoneId.of("UTC"));
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
    Date now = cal.getTime();
    Instant nowInstant = Instant.ofEpochMilli(now.getTime());
    ZonedDateTime nowZonedDateTime = ZonedDateTime.ofInstant(nowInstant, ZoneId.of("UTC"));
    int nowHour = nowZonedDateTime.getHour();

    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    triggerContext.update(now, now, now);
    Date actDate = trigger.nextExecutionTime(triggerContext);

    Instant actInstant = Instant.ofEpochMilli(actDate.getTime());
    ZonedDateTime actZonedDateTime = ZonedDateTime.ofInstant(actInstant, ZoneId.of("UTC"));
    int actHour = actZonedDateTime.getHour();
    assertEquals(actHour, nowHour + expDiffHours + addHours);
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
    triggerContext.update(null, null, null);
    Date actDate = trigger.nextExecutionTime(triggerContext);

    Instant actInstant = Instant.ofEpochMilli(actDate.getTime());
    ZonedDateTime actZonedDateTime = ZonedDateTime.ofInstant(actInstant, ZoneId.of("UTC"));
    int actHour = actZonedDateTime.getHour();
    assertEquals(nowHour + expDiffHours + addHours, actHour);
  }
}
