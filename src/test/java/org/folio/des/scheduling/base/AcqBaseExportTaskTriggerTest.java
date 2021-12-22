package org.folio.des.scheduling.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

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
    scheduleParameters.setScheduleTime("12:00:00.000Z");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);

    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNull(date);
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is set")
  void hourlyScheduleIfScheduledTimeIsSet() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(1);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR);
    scheduleParameters.setScheduleTime("12:00:00.000+00:00");
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);
    triggerContext.update(actDate, actDate,actDate);
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    int actHourLater =  actDateHourLate.getHours();
    var time = OffsetTime.parse("13:00:00.000Z", DateTimeFormatter.ISO_TIME);
    int expHour = LocalDate.now().atTime(time).getHour();
    assertEquals(expHour, actHourLater);
  }

  @Test
  @DisplayName("Hourly job scheduled if ScheduleTime is not set")
  void hourlyScheduleIfScheduledTimeIsNotSet() {
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setId(UUID.randomUUID());
    scheduleParameters.setScheduleFrequency(1);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR);
    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(scheduleParameters);
    //When
    SimpleTriggerContext triggerContext = new SimpleTriggerContext();
    final Date actDate = trigger.nextExecutionTime(triggerContext);
    triggerContext.update(actDate, actDate, new Date());
    final Date actDateHourLate = trigger.nextExecutionTime(triggerContext);
    int actHourLater = actDateHourLate.getHours();
    LocalDateTime hourLater = LocalDateTime.now().plusHours(1);
    int expHour = hourLater.getHour();
    assertEquals(expHour, actHourLater);
  }

//  @Test
//  @DisplayName("Hourly job scheduled for existing context")
//  void hourlyScheduleExisting() {
//    ExportConfig exportConfig = new ExportConfig();
//    exportConfig.setId(UUID.randomUUID().toString());
//    exportConfig.setScheduleFrequency(1);
//    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.HOUR);
//    ExportTrigger exportTrigger = new ExportTrigger();
//    exportTrigger.setConfig(exportConfig);
//    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(exportTrigger);
//
//    final Date now =  DateUtils.addHours(new Date(), 1);
//    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));
//
//    assertNotNull(date);
//  }
//
//  @Test
//  @DisplayName("Daily job scheduled")
//  void dailySchedule() {
//    ExportConfig exportConfig = new ExportConfig();
//
//    exportConfig.setId(UUID.randomUUID().toString());
//    exportConfig.setScheduleFrequency(1);
//    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.DAY);
//    exportConfig.setScheduleTime("12:00:00.000Z");
//    ExportTrigger exportTrigger = new ExportTrigger();
//    exportTrigger.setConfig(exportConfig);
//    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(exportTrigger);
//    trigger.nextExecutionTime(new SimpleTriggerContext());
//
//    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());
//
//    assertNotNull(date);
//  }
//
//  @Test
//  @DisplayName("Daily job scheduled for existing context")
//  void dailyScheduleExisting() {
//    ExportConfig exportConfig = new ExportConfig();
//    exportConfig.setId(UUID.randomUUID().toString());
//    exportConfig.setScheduleFrequency(1);
//    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.DAY);
//    exportConfig.setScheduleTime("12:00:00.000Z");
//    ExportTrigger exportTrigger = new ExportTrigger();
//    exportTrigger.setConfig(exportConfig);
//    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(exportTrigger);
//
//    final Date now = new Date();
//    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext(now, now, now));
//
//    assertNotNull(date);
//  }
//
//  @Test
//  @DisplayName("Weekly job scheduled")
//  void weeklySchedule() {
//    ExportConfig exportConfig = new ExportConfig();
//    exportConfig.setId(UUID.randomUUID().toString());
//    exportConfig.setScheduleFrequency(1);
//    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);
//    exportConfig.setScheduleTime("12:00:00.000Z");
//    ExportTrigger exportTrigger = new ExportTrigger();
//    exportTrigger.setConfig(exportConfig);
//    AcqBaseExportTaskTrigger trigger = new AcqBaseExportTaskTrigger(exportTrigger);
//    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());
//
//    assertNotNull(date);
//  }
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
