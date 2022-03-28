package org.folio.des.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.ExportConfig.WeekDaysEnum;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.support.SimpleTriggerContext;


@SpringBootTest(classes = {ExportTrigger.class})
class ExportTriggerTest {

  @Autowired private ExportTrigger trigger;

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

  private int adjustExpected(int expected) {
    if (expected >= 24) {
      expected -= 24;
    }
    return expected;
  }
}
