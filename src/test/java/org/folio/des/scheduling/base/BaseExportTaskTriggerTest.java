package org.folio.des.scheduling.base;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.ExportTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.SimpleTriggerContext;

class BaseExportTaskTriggerTest {

  @Test
  @DisplayName("Empty configuration for scheduling")
  void emptyConfig() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.NONE);
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
  }

  @Test
  @DisplayName("No jobs are scheduled")
  void noneScheduled() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setScheduleFrequency(1);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.NONE);
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);

    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());

    assertNull(instant);
  }

  @Test
  @DisplayName("Hourly job scheduled")
  void hourlySchedule() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setScheduleFrequency(1);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.HOUR);
    exportConfig.scheduleTime("15:06:00.000Z");
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);
    final Date now = new Date();
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext());
    Calendar nowPlusOneHour = Calendar.getInstance();
    nowPlusOneHour.setTime(now);
    nowPlusOneHour.add(Calendar.HOUR, 1);

    assertTrue(DateUtils.truncatedEquals(nowPlusOneHour.getTime(), Date.from(instant), Calendar.HOUR));
  }

  @Test
  @DisplayName("Hourly job scheduled for existing context")
  void hourlyScheduleExisting() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setScheduleFrequency(1);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.HOUR);
    exportConfig.scheduleTime("15:06:00.000Z");
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);

    final Instant now =  DateUtils.addHours(new Date(), 1).toInstant();
    final Instant date = trigger.nextExecution(new SimpleTriggerContext(now, now, now));

    assertNotNull(date);
  }

  @Test
  @DisplayName("Daily job scheduled")
  void dailySchedule() {
    ExportConfig exportConfig = new ExportConfig();

    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setScheduleFrequency(1);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.DAY);
    exportConfig.setScheduleTime("12:00:00.000Z");
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);
    trigger.nextExecution(new SimpleTriggerContext());

    final Instant date = trigger.nextExecution(new SimpleTriggerContext());

    assertNotNull(date);
  }

  @Test
  @DisplayName("Daily job scheduled for existing context")
  void dailyScheduleExisting() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setScheduleFrequency(1);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.DAY);
    exportConfig.setScheduleTime("12:00:00.000Z");
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);

    final Instant now = Instant.now();
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext(now, now, now));

    assertNotNull(instant);
  }

  @Test
  @DisplayName("Weekly job scheduled")
  void weeklySchedule() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setScheduleFrequency(1);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);
    exportConfig.setScheduleTime("12:00:00.000Z");
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);
    final Instant date = trigger.nextExecution(new SimpleTriggerContext());

    assertNotNull(date);
  }

  @Test
  @DisplayName("Weekly job scheduled for specific days")
  void weeklyScheduleWithWeekDays() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    exportConfig.setScheduleFrequency(1);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.DAY);
    exportConfig.setWeekDays(List.of(ExportConfig.WeekDaysEnum.FRIDAY));
    exportConfig.setScheduleTime("12:00:00.000Z");
    ExportTrigger exportTrigger = new ExportTrigger();
    exportTrigger.setConfig(exportConfig);
    BaseExportTaskTrigger trigger = new BaseExportTaskTrigger(exportTrigger);


    final Instant now = Instant.now();
    final Instant instant = trigger.nextExecution(new SimpleTriggerContext(now, now, now));

    assertNotNull(instant);
  }
}
