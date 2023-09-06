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
    log.debug("convert:: scheduleParameters={}, triggerGroup={}", scheduleParameters, triggerGroup);

    if (scheduleParameters == null) {
      log.debug("convert:: no schedule params provided.");
      return Collections.emptySet();
    }

    ScheduleParameters.SchedulePeriodEnum schedulePeriod = scheduleParameters.getSchedulePeriod();
    if (schedulePeriod == null || schedulePeriod == ScheduleParameters.SchedulePeriodEnum.NONE) {
      log.debug("convert:: no schedule period provided.");
      return Collections.emptySet();
    }

    return switch (schedulePeriod) {
      case HOUR -> buildHourlyTrigger(scheduleParameters, triggerGroup);
      case DAY -> buildDailyTrigger(scheduleParameters, triggerGroup);
      case WEEK -> buildWeeklyTrigger(scheduleParameters, triggerGroup);
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
      .map(dayOfWeek -> buildTrigger(parameters, ScheduleDateTimeUtil
        .convertScheduleTimeForWeekDayToDate(parameters, dayOfWeek), IntervalUnit.WEEK, triggerGroup))
      .collect(Collectors.toSet());
  }

  private Trigger buildTrigger(ScheduleParameters parameters, DateBuilder.IntervalUnit intervalUnit,
                               String triggerGroup) {

    return buildTrigger(parameters, ScheduleDateTimeUtil.convertScheduleTimeToDate(parameters),
      intervalUnit, triggerGroup);
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
