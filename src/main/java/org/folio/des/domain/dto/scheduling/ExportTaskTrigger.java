package org.folio.des.domain.dto.scheduling;

import java.time.DayOfWeek;
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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import org.folio.des.domain.dto.ScheduleParameters;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import lombok.Getter;

@EqualsAndHashCode
public class ExportTaskTrigger implements Trigger {

  @Getter
  private final String id;
  @Getter
  private final ScheduleParameters scheduleParameters;

  public ExportTaskTrigger(ScheduleParameters scheduleParameters) {
    this.id = UUID.randomUUID().toString();
    this.scheduleParameters = scheduleParameters;
  }

  public ExportTaskTrigger(String id, ScheduleParameters scheduleParameters) {
    this.id = id;
    this.scheduleParameters = scheduleParameters;
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
    return getNextTime(lastActualExecutionTime);
  }

//  @Override
//  public int hashCode() {
//    return Objects.hash(id);
//  }
//
//  @Override
//  public boolean equals(Object other) {
//    if (other == this) {
//      return true;
//    }
//    if (!(other instanceof ExportTaskTrigger)) {
//      return false;
//    }
//    ExportTaskTrigger trigger = ((ExportTaskTrigger) other);
//    return this.getId().equals(trigger.getId());
//  }

  private Date getNextTime(Date lastActualExecutionTime) {
    if (scheduleParameters == null) return null;

    ScheduleParameters.SchedulePeriodEnum schedulePeriod = scheduleParameters.getSchedulePeriod();
    if (schedulePeriod == null || schedulePeriod == ScheduleParameters.SchedulePeriodEnum.NONE) return null;

    Date nextExecutionTime;
    Integer scheduleFrequency = scheduleParameters.getScheduleFrequency();

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
    default:
      return null;
    }

    return nextExecutionTime;
  }

  private Date scheduleTaskWeekly(Date lastActualExecutionTime, Integer scheduleFrequency) {
    String scheduleTime = scheduleParameters.getScheduleTime();
    var time = OffsetTime.parse(scheduleTime, DateTimeFormatter.ISO_TIME);

    var offsetDateTime = LocalDate.now().atTime(time);

    if (lastActualExecutionTime == null) {
      return Date.from(offsetDateTime.toInstant());

    } else {
      var lastExecutionOffsetDateTime = OffsetDateTime.ofInstant(lastActualExecutionTime.toInstant(),
        ZoneId.systemDefault());
      var instant = findNextDayOfWeek(lastExecutionOffsetDateTime, scheduleFrequency).toInstant();
      return Date.from(instant);
    }
  }

  private OffsetDateTime findNextDayOfWeek(OffsetDateTime offsetDateTime, Integer weeks) {
    List<DayOfWeek> week = normalizeDayOfWeek();

    var currentDayOfWeek = offsetDateTime.getDayOfWeek();
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
    List<ScheduleParameters.WeekDaysEnum> weekDays = scheduleParameters.getWeekDays();
    return weekDays.stream().map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString())).sorted().collect(Collectors.toList());
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
    String scheduleTime = scheduleParameters.getScheduleTime();
    var time = OffsetTime.parse(scheduleTime, DateTimeFormatter.ISO_TIME);

    if (lastActualExecutionTime == null) {
      var nextExecutionDateTime = LocalDateTime.of(LocalDate.now(), time.toLocalTime());
      return Date.from(nextExecutionDateTime.toInstant(time.getOffset()));

    } else {
      var instant = lastActualExecutionTime.toInstant();
      var localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
      var newScheduledDate = localDateTime.plusDays(days);
      return Date.from(newScheduledDate.toInstant(time.getOffset()));
    }
  }
}
