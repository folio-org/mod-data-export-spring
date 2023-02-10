package org.folio.des.scheduling;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.ExportConfig.WeekDaysEnum;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
public class ExportTrigger implements Trigger {

  @Setter
  @Getter
  private ExportConfig config;

  @Override
  public Instant nextExecution(TriggerContext triggerContext) {
    Instant lastActualExecutionTime = triggerContext.lastActualExecution();
    return getNextTime(lastActualExecutionTime);
  }

  protected Instant getNextTime(Instant lastActualExecutionTime) {
    if (config == null) return null;

    SchedulePeriodEnum schedulePeriod = config.getSchedulePeriod();
    if (schedulePeriod == null || schedulePeriod == SchedulePeriodEnum.NONE) return null;

    Instant nextExecutionTime;
    Integer scheduleFrequency = config.getScheduleFrequency();

    switch (schedulePeriod) {
      case DAY -> nextExecutionTime = scheduleTaskWithDayPeriod(lastActualExecutionTime, scheduleFrequency);
      case WEEK -> nextExecutionTime = scheduleTaskWeekly(lastActualExecutionTime, scheduleFrequency);
      case HOUR -> nextExecutionTime = scheduleTaskWithHourPeriod(lastActualExecutionTime, scheduleFrequency);
      default -> {
        return null;
      }
    }

    return nextExecutionTime;
  }

  private Instant scheduleTaskWeekly(Instant lastActualExecutionTime, Integer scheduleFrequency) {
    String scheduleTime = config.getScheduleTime();
    var time = OffsetTime.parse(scheduleTime, DateTimeFormatter.ISO_TIME);

    var offsetDateTime = LocalDate.now().atTime(time);

    if (lastActualExecutionTime == null) {
      return offsetDateTime.toInstant();

    } else {
      var lastExecutionOffsetDateTime = OffsetDateTime.ofInstant(lastActualExecutionTime,
          ZoneId.systemDefault());
      return findNextDayOfWeek(lastExecutionOffsetDateTime, scheduleFrequency).toInstant();
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
    List<WeekDaysEnum> weekDays = config.getWeekDays();
    return weekDays.stream().map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString())).sorted().toList();
  }

  private Instant scheduleTaskWithHourPeriod(Instant lastActualExecutionTime, Integer hours) {
    Calendar nextExecutionTime = new GregorianCalendar();
    Date nowDate = new Date();
    if (lastActualExecutionTime == null) {
      nextExecutionTime.setTime(nowDate);
      nextExecutionTime.add(Calendar.HOUR, 1);
    } else {
      nextExecutionTime.setTimeInMillis(lastActualExecutionTime.toEpochMilli());
      long diffHours = (nowDate.getTime() - nextExecutionTime.getTime().getTime())/(60 * 60 * 1000);
      if (diffHours > 0 && hours !=0 && diffHours > hours) {
        BigDecimal hoursToIncrease = BigDecimal.valueOf(diffHours)
                                               .divide(BigDecimal.valueOf(hours), RoundingMode.FLOOR)
                                               .add(BigDecimal.ONE);
        nextExecutionTime.add(Calendar.HOUR, hoursToIncrease.intValue() * hours);
      } else {
        nextExecutionTime.add(Calendar.HOUR, hours);
      }
    }
    return nextExecutionTime.toInstant();
  }

  private Instant scheduleTaskWithDayPeriod(Instant lastActualExecutionTime, Integer days) {
    String scheduleTime = config.getScheduleTime();
    var time = OffsetTime.parse(scheduleTime, DateTimeFormatter.ISO_TIME);

    if (lastActualExecutionTime == null) {
      var nextExecutionDateTime = LocalDateTime.of(LocalDate.now(), time.toLocalTime());
      return nextExecutionDateTime.toInstant(time.getOffset());

    } else {
      var localDateTime = LocalDateTime.ofInstant(lastActualExecutionTime, ZoneId.systemDefault());
      var newScheduledDate = localDateTime.plusDays(days);
      return newScheduledDate.toInstant(time.getOffset());
    }
  }

}
