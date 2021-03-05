package org.folio.des.scheduling;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Setter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.ExportConfig.WeekDaysEnum;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;

@Component
public class BursarExportTrigger implements Trigger {

  @Setter
  private ExportConfig config;

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
    return getNextTime(lastActualExecutionTime);
  }

  private Date getNextTime(Date lastActualExecutionTime) {
    if (config == null) return null;
    SchedulePeriodEnum schedulePeriod = config.getSchedulePeriod();
    if (schedulePeriod == SchedulePeriodEnum.NONE) return null;
    if (config.getExportTypeSpecificParameters() == null) return null;
    if (config.getExportTypeSpecificParameters().getBursarFeeFines() == null) return null;

    Date nextExecutionTime = null;
    Integer scheduleFrequency = config.getScheduleFrequency();

    switch (schedulePeriod) {
      case DAY:
        nextExecutionTime = scheduleTaskWithDayPeriod(lastActualExecutionTime, scheduleFrequency);
        break;
      case WEEK:
        nextExecutionTime = scheduleTaskWeekly(lastActualExecutionTime, scheduleFrequency);
        break;
      case HOUR:
        nextExecutionTime = scheduleTaskWithHourPeriod(lastActualExecutionTime, scheduleFrequency);
        break;
    }

    return nextExecutionTime;
  }

  private Date scheduleTaskWeekly(Date lastActualExecutionTime, Integer scheduleFrequency) {
    String scheduleTime = config.getScheduleTime();
    var time = OffsetTime.parse(scheduleTime, DateTimeFormatter.ISO_TIME);

    OffsetDateTime offsetDateTime = LocalDate.now().atTime(time);

    if (lastActualExecutionTime == null) {
      return Date.from(offsetDateTime.toInstant());

    } else {
      OffsetDateTime lastExecutionOffsetDateTime =
          OffsetDateTime.ofInstant(lastActualExecutionTime.toInstant(), ZoneId.systemDefault());
      Instant instant =
          findNextDayOfWeek(lastExecutionOffsetDateTime, scheduleFrequency).toInstant();
      return Date.from(instant);
    }
  }

  private OffsetDateTime findNextDayOfWeek(OffsetDateTime offsetDateTime, Integer weeks) {
    List<DayOfWeek> week = normalizeDayOfWeek();

    DayOfWeek currentDayOfWeek = offsetDateTime.getDayOfWeek();
    for (DayOfWeek ofWeek : week) {
      int nextWeekDay = currentDayOfWeek.getValue() - ofWeek.getValue();
      if (nextWeekDay >= 1) {
        return offsetDateTime.plusDays(nextWeekDay);
      }
    }
    int daysBefore = week.get(0).getValue() - currentDayOfWeek.getValue();

    return offsetDateTime.minusDays(daysBefore).plusWeeks(weeks);
  }

  private List<DayOfWeek> normalizeDayOfWeek() {
    List<WeekDaysEnum> weekDays = config.getWeekDays();
    return weekDays.stream()
        .map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString()))
        .sorted()
        .collect(Collectors.toList());
  }

  private Date scheduleTaskWithHourPeriod(Date lastActualExecutionTime, Integer hours) {
    Calendar nextExecutionTime = new GregorianCalendar();
    if (lastActualExecutionTime == null) {
      nextExecutionTime.setTime(new Date());
    } else {
      nextExecutionTime.setTime(lastActualExecutionTime);
      nextExecutionTime.add(Calendar.HOUR, hours);
    }
    return nextExecutionTime.getTime();
  }

  private Date scheduleTaskWithDayPeriod(Date lastActualExecutionTime, Integer days) {
    String scheduleTime = config.getScheduleTime();
    var time = OffsetTime.parse(scheduleTime, DateTimeFormatter.ISO_TIME);

    if (lastActualExecutionTime == null) {
      LocalDateTime nextExecutionDateTime = LocalDateTime.of(LocalDate.now(), time.toLocalTime());
      return Date.from(nextExecutionDateTime.toInstant(time.getOffset()));

    } else {
      Instant instant = lastActualExecutionTime.toInstant();
      LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
      LocalDateTime newScheduledDate = localDateTime.plusDays(days);
      return Date.from(newScheduledDate.toInstant(time.getOffset()));
    }
  }
}
