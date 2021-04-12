package org.folio.des.scheduling;

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
    trigger.setConfig(new ExportConfig());

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
    config.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());
    trigger.setConfig(config);

    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertTrue(DateUtils.truncatedEquals(now, date, Calendar.SECOND));
  }

  @Test
  @DisplayName("Hourly job scheduled for existing context")
  void hourlyScheduleExisting() {
    ExportConfig config = new ExportConfig();
    final int scheduleFrequency = 1;
    config.setScheduleFrequency(scheduleFrequency);
    config.setSchedulePeriod(SchedulePeriodEnum.HOUR);
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

    final Date now = new Date();
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

    final Date now = new Date();
    final Date date = trigger.nextExecutionTime(new SimpleTriggerContext());

    assertNotNull(date);
  }

  @Test
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
}
