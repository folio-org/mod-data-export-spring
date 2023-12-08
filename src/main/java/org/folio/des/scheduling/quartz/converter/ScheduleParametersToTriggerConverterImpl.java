package org.folio.des.scheduling.quartz.converter;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.util.ScheduleDateTimeUtil;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ScheduleParametersToTriggerConverterImpl implements ScheduleParametersToTriggerConverter {
  @Override
  public Set<Trigger> convert(ScheduleParameters scheduleParameters, String triggerGroup) {

    if (scheduleParameters == null) {
      return Collections.emptySet();
    }

    ScheduleParameters.SchedulePeriodEnum schedulePeriod = scheduleParameters.getSchedulePeriod();
    if (schedulePeriod == null || schedulePeriod == ScheduleParameters.SchedulePeriodEnum.NONE) {
      return Collections.emptySet();
    }

    return switch (schedulePeriod) {
      case HOUR -> buildHourlyTrigger(scheduleParameters, triggerGroup);
      case DAY -> buildDailyTrigger(scheduleParameters, triggerGroup);
      case WEEK -> buildWeeklyTrigger(scheduleParameters, triggerGroup);
      case MONTH -> buildMonthlyTrigger(scheduleParameters, triggerGroup);
      default -> Collections.emptySet();
    };
  }

  private Set<Trigger> buildHourlyTrigger(ScheduleParameters parameters, String triggerGroup) {
    return Set.of(buildTrigger(parameters, IntervalUnit.HOUR, triggerGroup));
  }

  private Set<Trigger> buildDailyTrigger(ScheduleParameters parameters, String triggerGroup) {
    return Set.of(buildTrigger(parameters, IntervalUnit.DAY, triggerGroup));
  }

  private Set<Trigger> buildWeeklyTrigger(ScheduleParameters parameters, String triggerGroup) {
    return getDaysOfWeek(parameters).stream()
      .map(dayOfWeek -> {
        Date startTime = ScheduleDateTimeUtil.convertScheduleTimeForWeekDayToDate(parameters, dayOfWeek);
        return buildTrigger(parameters, startTime, IntervalUnit.WEEK, triggerGroup);
      })
      .collect(Collectors.toSet());
  }

  private Set<Trigger> buildMonthlyTrigger(ScheduleParameters parameters, String triggerGroup) {
    Date startTime = ScheduleDateTimeUtil.convertScheduleTimeForMonthDayToDate(parameters);
    return Set.of(buildTrigger(parameters, startTime, IntervalUnit.MONTH, triggerGroup));
  }

  private Trigger buildTrigger(ScheduleParameters parameters, DateBuilder.IntervalUnit intervalUnit,
                               String triggerGroup) {

    Date startTime = ScheduleDateTimeUtil.convertScheduleTimeToDate(parameters);
    return buildTrigger(parameters, startTime, intervalUnit, triggerGroup);
  }

  private Trigger buildTrigger(ScheduleParameters parameters, Date startTime, IntervalUnit intervalUnit,
                               String triggerGroup) {
    log.debug("buildTrigger:: Start Time is:{}", startTime);
    return TriggerBuilder.newTrigger()
      .withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
        .withInterval(parameters.getScheduleFrequency(), intervalUnit)
        .inTimeZone(TimeZone.getTimeZone(parameters.getTimeZone()))
        .preserveHourOfDayAcrossDaylightSavings(true)
        .withMisfireHandlingInstructionDoNothing())
      .withIdentity(UUID.randomUUID().toString(), triggerGroup)
      .startAt(startTime)
      .build();
  }

  private List<DayOfWeek> getDaysOfWeek(ScheduleParameters scheduleParameters) {
    return Optional.ofNullable(scheduleParameters.getWeekDays())
      .orElse(Collections.emptyList())
      .stream()
      .map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString()))
      .distinct()
      .toList();
  }
}
