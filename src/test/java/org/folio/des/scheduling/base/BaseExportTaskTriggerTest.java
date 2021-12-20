package org.folio.des.scheduling.base;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.des.domain.dto.ScheduleParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.SimpleTriggerContext;

class BaseExportTaskTriggerTest {


  @Test
  @DisplayName("No configuration for scheduling")
  void noConfig() {
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(null);
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("No jobs are scheduled")
  void noneScheduled() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    scheduledParams.setId(UUID.randomUUID());
    scheduledParams.setScheduleFrequency(1);
    scheduledParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Hourly job scheduled")
  void hourlySchedule() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    scheduledParams.setId(UUID.randomUUID());
    scheduledParams.setScheduleFrequency(1);
    scheduledParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);
    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertTrue(DateUtils.truncatedEquals(now, date, Calendar.SECOND));
  }

  @Test
  @DisplayName("Hourly job scheduled for existing context")
  void hourlyScheduleExisting() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    scheduledParams.setId(UUID.randomUUID());
    scheduledParams.setScheduleFrequency(1);
    scheduledParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);

    final Date now =  DateUtils.addHours(new Date(), 1);
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));

    assertNotNull(date);
  }

  @Test
  @DisplayName("Daily job scheduled")
  void dailySchedule() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    scheduledParams.setId(UUID.randomUUID());
    scheduledParams.setScheduleFrequency(1);
    scheduledParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.DAY);
    scheduledParams.setScheduleTime("12:00:00.000Z");
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNotNull(date);
  }

  @Test
  @DisplayName("Daily job scheduled for existing context")
  void dailyScheduleExisting() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    scheduledParams.setId(UUID.randomUUID());
    scheduledParams.setScheduleFrequency(1);
    scheduledParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.DAY);
    scheduledParams.setScheduleTime("12:00:00.000Z");
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);

    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));

    assertNotNull(date);
  }

  @Test
  @DisplayName("Weekly job scheduled")
  void weeklySchedule() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    scheduledParams.setId(UUID.randomUUID());
    scheduledParams.setScheduleFrequency(1);
    scheduledParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    scheduledParams.setScheduleTime("12:00:00.000Z");
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNotNull(date);
  }

  @Test
  @DisplayName("Weekly job scheduled for specific days")
  void weeklyScheduleWithWeekDays() {
    ScheduleParameters scheduledParams = new ScheduleParameters();
    scheduledParams.setId(UUID.randomUUID());
    scheduledParams.setScheduleFrequency(1);
    scheduledParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.DAY);
    scheduledParams.setWeekDays(List.of(ScheduleParameters.WeekDaysEnum.FRIDAY));
    scheduledParams.setScheduleTime("12:00:00.000Z");
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(scheduledParams);


    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));

    assertNotNull(date);
  }
}
