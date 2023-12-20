package org.folio.des.scheduling.quartz.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum;
import org.folio.des.domain.dto.ScheduleParameters.WeekDaysEnum;
import org.junit.jupiter.api.Test;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.Trigger;

import lombok.extern.log4j.Log4j2;

@Log4j2
class ScheduleParametersToTriggerConverterImplTest {
  private static final String ASIA_SHANGHAI_ZONE = "Asia/Shanghai";
  private static final ZoneId ASIA_SHANGHAI_ZONE_ID = ZoneId.of(ASIA_SHANGHAI_ZONE);
  private static final String EDIFACT_ORDERS_EXPORT = "edifactOrdersExport";
  private final ScheduleParametersToTriggerConverter triggerConverter = new ScheduleParametersToTriggerConverterImpl();

  @Test
  void testNullScheduleParameters() {
    var triggers = triggerConverter.convert(null, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertTrue(triggers.isEmpty());
  }

  @Test
  void testEmptyScheduleParameters() {
    var triggers = triggerConverter.convert(new ScheduleParameters(), EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertTrue(triggers.isEmpty());
  }

  @Test
  void testNotScheduled() {
    var triggers = triggerConverter.convert(buildScheduleParameters().schedulePeriod(SchedulePeriodEnum.NONE),
      EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertTrue(triggers.isEmpty());
  }

  @Test
  void convertToHourlyTrigger() {
    int scheduleFrequency = 8;
    ZonedDateTime expectedStartDateTime = ZonedDateTime.of(LocalDate.now(ASIA_SHANGHAI_ZONE_ID),
      LocalTime.of(11, 12, 3), ASIA_SHANGHAI_ZONE_ID);

    ScheduleParameters scheduleParameters = new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("11:12:03")
      .timeZone(ASIA_SHANGHAI_ZONE)
      .schedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR)
      .scheduleFrequency(scheduleFrequency);

    var triggers = triggerConverter.convert(scheduleParameters, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertEquals(1, triggers.size());

    var trigger = triggers.iterator().next();
    validateTriggerWithFirstFirings(trigger, expectedStartDateTime, ChronoUnit.HOURS, scheduleFrequency);
  }

  @Test
  void testConvertToDailyTrigger() {
    int scheduleFrequency = 13;
    ZonedDateTime expectedStartDateTime = ZonedDateTime.of(LocalDate.now(ASIA_SHANGHAI_ZONE_ID),
      LocalTime.of(16, 40, 59), ASIA_SHANGHAI_ZONE_ID);

    ScheduleParameters scheduleParameters = new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("16:40:59")
      .timeZone(ASIA_SHANGHAI_ZONE)
      .schedulePeriod(SchedulePeriodEnum.DAY)
      .scheduleFrequency(scheduleFrequency);

    var triggers = triggerConverter.convert(scheduleParameters, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertEquals(1, triggers.size());

    var trigger = triggers.iterator().next();
    validateTriggerWithFirstFirings(trigger, expectedStartDateTime, ChronoUnit.DAYS, scheduleFrequency);
  }

  @Test
  void testConvertToWeeklyTrigger() {
    int scheduleFrequency = 2;
    ZonedDateTime expectedStartDateTime = ZonedDateTime.of(
      LocalDate.now(ASIA_SHANGHAI_ZONE_ID).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)),
      LocalTime.of(6, 30, 0), ASIA_SHANGHAI_ZONE_ID);

    ScheduleParameters scheduleParameters = new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("06:30:00")
      .timeZone(ASIA_SHANGHAI_ZONE)
      .schedulePeriod(SchedulePeriodEnum.WEEK)
      .weekDays(List.of(WeekDaysEnum.MONDAY))
      .scheduleFrequency(scheduleFrequency);

    var triggers = triggerConverter.convert(scheduleParameters, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertEquals(1, triggers.size());

    var trigger = triggers.iterator().next();
    validateTriggerWithFirstFirings(trigger, expectedStartDateTime, ChronoUnit.WEEKS, scheduleFrequency);
  }

  @Test
  void testConvertToMonthlyTrigger() {
    ScheduleParameters scheduleParameters = new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("16:40:59")
      .timeZone(ASIA_SHANGHAI_ZONE)
      .schedulePeriod(SchedulePeriodEnum.MONTH)
      .scheduleDay(22)
      .scheduleFrequency(7);

    var triggers = triggerConverter.convert(scheduleParameters, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertEquals(1, triggers.size());

    var trigger = triggers.iterator().next();
    ZonedDateTime expectedStartDateTime = ZonedDateTime.of(
      LocalDate.now(ASIA_SHANGHAI_ZONE_ID).withDayOfMonth(22),
      LocalTime.of(16, 40, 59), ASIA_SHANGHAI_ZONE_ID
    );
    validateTriggerWithFirstFirings(trigger, expectedStartDateTime, ChronoUnit.MONTHS, 7);
  }

  @Test
  void testConvertToMonthlyTriggerWithNullParams() {
    ScheduleParameters scheduleParameters = new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("16:40:59")
      .timeZone(ASIA_SHANGHAI_ZONE)
      .schedulePeriod(SchedulePeriodEnum.MONTH)
      .scheduleFrequency(7);

    var triggers = triggerConverter.convert(scheduleParameters, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertEquals(1, triggers.size());

    var trigger = triggers.iterator().next();
    ZonedDateTime expectedStartDateTime = ZonedDateTime.of(
      Instant.now().atZone(ZoneId.of("UTC")).toLocalDate(),
      LocalTime.of(16, 40, 59), ASIA_SHANGHAI_ZONE_ID
    );
    validateTriggerWithFirstFirings(trigger, expectedStartDateTime, ChronoUnit.MONTHS, 7);
  }

  @Test
  void testConvertToWeeklyTriggerForSeveralDays() {
    int scheduleFrequency = 2;
    ZonedDateTime expectedStartDateTimeTrigger1 = ZonedDateTime.of(
      LocalDate.now(ASIA_SHANGHAI_ZONE_ID).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)),
      LocalTime.of(8, 15, 0), ASIA_SHANGHAI_ZONE_ID);
    ZonedDateTime expectedStartDateTimeTrigger2 = ZonedDateTime.of(
      LocalDate.now(ASIA_SHANGHAI_ZONE_ID).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)),
      LocalTime.of(8, 15, 0), ASIA_SHANGHAI_ZONE_ID);
    ZonedDateTime expectedStartDateTimeTrigger3 = ZonedDateTime.of(
      LocalDate.now(ASIA_SHANGHAI_ZONE_ID).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)),
      LocalTime.of(8, 15, 0), ASIA_SHANGHAI_ZONE_ID);

    ScheduleParameters scheduleParameters = new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("08:15:00")
      .timeZone(ASIA_SHANGHAI_ZONE)
      .schedulePeriod(SchedulePeriodEnum.WEEK)
      .weekDays(List.of(WeekDaysEnum.SATURDAY, WeekDaysEnum.TUESDAY, WeekDaysEnum.FRIDAY))
      .scheduleFrequency(scheduleFrequency);

    var triggers = triggerConverter.convert(scheduleParameters, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);
    assertEquals(3, triggers.size());

    var trigger1 = findTriggerWithDayOfWeek(triggers, DayOfWeek.TUESDAY);
    assertNotNull(trigger1);
    var trigger2 = findTriggerWithDayOfWeek(triggers, DayOfWeek.FRIDAY);
    assertNotNull(trigger2);
    var trigger3 = findTriggerWithDayOfWeek(triggers, DayOfWeek.SATURDAY);
    assertNotNull(trigger3);

    validateTriggerWithFirstFirings(trigger1, expectedStartDateTimeTrigger1, ChronoUnit.WEEKS, scheduleFrequency);
    validateTriggerWithFirstFirings(trigger2, expectedStartDateTimeTrigger2, ChronoUnit.WEEKS, scheduleFrequency);
    validateTriggerWithFirstFirings(trigger3, expectedStartDateTimeTrigger3, ChronoUnit.WEEKS, scheduleFrequency);
  }

  @Test
  void testConvertToWeeklyTriggerShouldSkipDuplicateDays() {
    int scheduleFrequency = 3;
    ZonedDateTime expectedStartDateTimeTrigger1 = ZonedDateTime.of(
      LocalDate.now(ASIA_SHANGHAI_ZONE_ID).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)),
      LocalTime.of(8, 15, 0), ASIA_SHANGHAI_ZONE_ID);
    ZonedDateTime expectedStartDateTimeTrigger2 = ZonedDateTime.of(
      LocalDate.now(ASIA_SHANGHAI_ZONE_ID).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
      LocalTime.of(8, 15, 0), ASIA_SHANGHAI_ZONE_ID);

    ScheduleParameters scheduleParameters = new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("08:15:00")
      .timeZone(ASIA_SHANGHAI_ZONE)
      .schedulePeriod(SchedulePeriodEnum.WEEK)
      .weekDays(List.of(WeekDaysEnum.SUNDAY, WeekDaysEnum.MONDAY, WeekDaysEnum.MONDAY, WeekDaysEnum.SUNDAY))
      .scheduleFrequency(scheduleFrequency);

    var triggers = triggerConverter.convert(scheduleParameters, EDIFACT_ORDERS_EXPORT);
    assertNotNull(triggers);

    assertEquals(2, triggers.size());
    var trigger1 = findTriggerWithDayOfWeek(triggers, DayOfWeek.MONDAY);
    assertNotNull(trigger1);
    var trigger2 = findTriggerWithDayOfWeek(triggers, DayOfWeek.SUNDAY);
    assertNotNull(trigger2);

    validateTriggerWithFirstFirings(trigger1, expectedStartDateTimeTrigger1, ChronoUnit.WEEKS, scheduleFrequency);
    validateTriggerWithFirstFirings(trigger2, expectedStartDateTimeTrigger2, ChronoUnit.WEEKS, scheduleFrequency);
  }

  private ScheduleParameters buildScheduleParameters() {
    return new ScheduleParameters()
      .id(UUID.randomUUID())
      .scheduleTime("08:00:00")
      .timeZone("CET")
      .schedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR)
      .scheduleFrequency(2);
  }

  private void validateTriggerWithFirstFirings(Trigger trigger, ZonedDateTime firstFiringTimeExpected,
                                               TemporalUnit scheduleUnit, int frequency) {

    ZonedDateTime secondFiringTimeExpected = firstFiringTimeExpected.plus(frequency, scheduleUnit);
    ZonedDateTime thirdFiringTimeExpected = secondFiringTimeExpected.plus(frequency, scheduleUnit);

    assertNotNull(trigger.getKey().getName());
    assertEquals(EDIFACT_ORDERS_EXPORT, trigger.getKey().getGroup());
    assertEquals(CalendarIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, trigger.getMisfireInstruction());

    ZonedDateTime triggerStartDateTime = getZonedDateTime(trigger.getStartTime(), ASIA_SHANGHAI_ZONE_ID);
    log.info("trigger start time: " + triggerStartDateTime);
    assertEquals(firstFiringTimeExpected, triggerStartDateTime);

    ZonedDateTime firstTriggerFireTime = trigger.getFireTimeAfter(Date
      .from(firstFiringTimeExpected.minusSeconds(1).toInstant())).toInstant().atZone(ASIA_SHANGHAI_ZONE_ID);
    log.info("trigger first fire time: " + firstTriggerFireTime);
    assertEquals(firstFiringTimeExpected, firstTriggerFireTime);

    ZonedDateTime secondTriggerFireTime = trigger.getFireTimeAfter(Date.from(firstFiringTimeExpected.toInstant()))
      .toInstant().atZone(ASIA_SHANGHAI_ZONE_ID);
    log.info("trigger second fire time: " + secondTriggerFireTime);
    assertEquals(secondFiringTimeExpected, secondTriggerFireTime);

    ZonedDateTime thirdTriggerFireTime = trigger.getFireTimeAfter(Date.from(secondFiringTimeExpected.toInstant()))
      .toInstant().atZone(ASIA_SHANGHAI_ZONE_ID);
    log.info("trigger third fire time: " + thirdTriggerFireTime);
    assertEquals(thirdFiringTimeExpected, thirdTriggerFireTime);
  }

  private ZonedDateTime getZonedDateTime(Date date, ZoneId zoneId) {
    return date.toInstant().atZone(zoneId);
  }

  private Trigger findTriggerWithDayOfWeek(Set<Trigger> triggers, DayOfWeek dayOfWeek) {
    return triggers.stream().filter(trigger -> getZonedDateTime(trigger.getStartTime(), ASIA_SHANGHAI_ZONE_ID)
      .getDayOfWeek() == dayOfWeek).findFirst().orElse(null);
  }
}
