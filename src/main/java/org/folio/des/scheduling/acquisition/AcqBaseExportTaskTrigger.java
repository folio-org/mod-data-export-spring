package org.folio.des.scheduling.acquisition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.ScheduleParameters.WeekDaysEnum;
import org.folio.des.scheduling.base.AbstractExportTaskTrigger;
import org.folio.des.scheduling.base.ScheduleDateTimeUtil;
import org.springframework.scheduling.TriggerContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class AcqBaseExportTaskTrigger extends AbstractExportTaskTrigger {
  @Getter
  private final boolean enableScheduler;
  @Getter
  private final Date lastJobStartDate;

  public AcqBaseExportTaskTrigger(ScheduleParameters scheduleParameters, Date lastJobStartDate, boolean enableScheduler) {
    this.scheduleParameters = scheduleParameters;
    this.lastJobStartDate = lastJobStartDate;
    this.enableScheduler = enableScheduler;
  }

  @Override
  public ScheduleParameters getScheduleParameters() {
    return this.scheduleParameters;
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
    if (lastActualExecutionTime == null && lastJobStartDate != null)  {
      lastActualExecutionTime = lastJobStartDate;
    }
    return getNextTime(lastActualExecutionTime);
  }

  @Override
  public boolean isDisabledSchedule() {
    return Optional.ofNullable(scheduleParameters)
                  .map(ScheduleParameters::getSchedulePeriod)
                  .map(ScheduleParameters.SchedulePeriodEnum.NONE::equals)
                  .orElse(false) || !enableScheduler;
  }

  private Date getNextTime(Date lastActualExecutionTime) {
    if (scheduleParameters == null) return null;

    ScheduleParameters.SchedulePeriodEnum schedulePeriod = scheduleParameters.getSchedulePeriod();
    if (schedulePeriod == null || schedulePeriod == ScheduleParameters.SchedulePeriodEnum.NONE) {
      return null;
    }

    Integer scheduleFrequency = scheduleParameters.getScheduleFrequency();

    switch (schedulePeriod) {
    case DAY:
      return scheduleTaskWithDayPeriod(lastActualExecutionTime, scheduleFrequency);
    case HOUR:
      return scheduleTaskWithHourPeriod(lastActualExecutionTime, scheduleFrequency);
    case WEEK:
      return scheduleTaskWeekly(lastActualExecutionTime, scheduleFrequency);
    default:
      return null;
    }
  }

  @SneakyThrows
  private Date scheduleTaskWeekly(Date lastActualExecutionTime, Integer weeksFrequency) {
    ZonedDateTime startTime = ScheduleDateTimeUtil.convertScheduleTime(lastActualExecutionTime, scheduleParameters);
    startTime = findNextDayOfWeek(startTime, weeksFrequency);
    log.info("Weekly next schedule execution time in UTC for config {} is : {}", scheduleParameters.getId(), startTime);
    return ScheduleDateTimeUtil.convertToOldDateFormat(startTime, scheduleParameters);
  }

  private ZonedDateTime findNextDayOfWeek(ZonedDateTime scheduleDateTime, Integer weeksFrequency) {
    List<DayOfWeek> sortedChosenDays = normalizeAndSortDayOfWeek();

    ZonedDateTime currentDateTime = getNowDateTime(scheduleParameters.getTimeZone());
    DayOfWeek currentDay = currentDateTime.getDayOfWeek();

    if (CollectionUtils.isNotEmpty(sortedChosenDays)) {
      // iterate through chosen days
      for (DayOfWeek chosenDay : sortedChosenDays) {
        if (currentDay == chosenDay) {
          if (currentDateTime.isBefore(scheduleDateTime)) {
            // run scheduler in the same day
            return scheduleDateTime;
          } else if (sortedChosenDays.size() == 1) {
            // run scheduler on the same day but after weeksFrequency weeks
            return scheduleDateTime.plusWeeks(weeksFrequency);
          } else if (isLastChosenDay(sortedChosenDays, chosenDay)) {
            // run next weeks depends on weeksFrequency
            return runNextWeeks(scheduleDateTime, sortedChosenDays, currentDay, weeksFrequency);
          }
        } else if (chosenDay.getValue() > currentDay.getValue()) {
          // run scheduler on the same day on the same week
          int delta = chosenDay.getValue() - currentDay.getValue();
          return scheduleDateTime.plusDays(delta);
        } else if (!containsDaysAfter(sortedChosenDays, chosenDay)) {
          // run next weeks depends on weeksFrequency
          return runNextWeeks(scheduleDateTime, sortedChosenDays, currentDay, weeksFrequency);
        }
      }
    }
    return null;
  }

  private List<DayOfWeek> normalizeAndSortDayOfWeek() {
    List<WeekDaysEnum> weekDays = scheduleParameters.getWeekDays();
    return weekDays.stream()
            .sorted(Comparator.comparing(WeekDaysEnum::getValue))
            .map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString())).sorted().collect(Collectors.toList());
  }

  private boolean containsDaysAfter(List<DayOfWeek> sortedChosenDays, DayOfWeek chosenDay) {
    return sortedChosenDays.stream().anyMatch(dayOfWeek -> dayOfWeek.getValue() > chosenDay.getValue());
  }

  private boolean isLastChosenDay(List<DayOfWeek> sortedChosenDays, DayOfWeek chosenDay) {
    return sortedChosenDays.get(sortedChosenDays.size() - 1) == chosenDay;
  }

  private ZonedDateTime runNextWeeks(ZonedDateTime scheduleDateTime,
                                     List<DayOfWeek> sortedChosenDays,
                                     DayOfWeek currentDay,
                                     Integer weeksFrequency) {
    long daysToSunday = (long) DayOfWeek.SUNDAY.getValue() - currentDay.getValue();
    DayOfWeek firstDayFromNextWeek = sortedChosenDays.get(0);
    return scheduleDateTime
      .plusDays(daysToSunday + firstDayFromNextWeek.getValue())
      .plusWeeks(weeksFrequency - 1L);
  }

  @SneakyThrows
  private Date scheduleTaskWithHourPeriod(Date lastActualExecutionTime, Integer hours) {
    ZonedDateTime startTime = ScheduleDateTimeUtil.convertScheduleTime(lastActualExecutionTime, scheduleParameters);

    if (lastActualExecutionTime != null) {
      ZoneId zoneId = ZoneId.of("UTC");
      ZonedDateTime nowDate = Instant.now().atZone(zoneId);
      long diffHours = (nowDate.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli())/(60 * 60 * 1000);
      if (diffHours > 0 && hours !=0 && diffHours > hours) {
        BigDecimal hoursToIncrease = BigDecimal.valueOf(diffHours)
          .divide(BigDecimal.valueOf(hours), RoundingMode.FLOOR)
          .add(BigDecimal.ONE);
        startTime = startTime.plusHours(hoursToIncrease.longValue() * hours);
      } else {
        startTime = startTime.plusHours(hours);
      }
    }

    startTime = normalizeIfNextRunInPast(startTime, getNowDateTime(scheduleParameters.getTimeZone()));

    log.info("Hourly next schedule execution time in {} for config {} is : {}", scheduleParameters.getTimeZone(), scheduleParameters.getId(), startTime);
    return ScheduleDateTimeUtil.convertToOldDateFormat(startTime, scheduleParameters);
  }

  @SneakyThrows
  private Date scheduleTaskWithDayPeriod(Date lastActualExecutionTime, Integer days) {
    ZonedDateTime startTime = ScheduleDateTimeUtil.convertScheduleTime(lastActualExecutionTime, scheduleParameters);
    if (lastActualExecutionTime != null) {
      startTime = startTime.plusDays(days);
    }

    log.info("Day next schedule execution time in {} for config {} is : {}", scheduleParameters.getTimeZone(), scheduleParameters.getId(), startTime);
    return ScheduleDateTimeUtil.convertToOldDateFormat(startTime, scheduleParameters);
  }

  /**
   * This method normalized date because if date was in past - spring scheduler invoked it now, that is not the desired behaviour.
   * Some examples for this new logic:
   * User selects 13.30 as a start time and current time is 16.10 - next run will be at 16.30
   * User selects 13.10 as a start time and current time is 16.40 - next run will be at 17.10
   *
   * @param startTime the start time
   * @param nowTime the now time
   * @return normalized time
   */
  private ZonedDateTime normalizeIfNextRunInPast(ZonedDateTime startTime, ZonedDateTime nowTime) {
    if (startTime.isBefore(nowTime)) {
      if (startTime.getMinute() > nowTime.getMinute()) {
        return nowTime.withMinute(startTime.getMinute());
      } else {
        return nowTime.plusHours(1).withMinute(startTime.getMinute());
      }
    }
    return startTime;
  }

  private ZonedDateTime getNowDateTime(String timeZoneId) {
    ZoneId zoneId = ZoneId.of(StringUtils.defaultIfBlank(timeZoneId, "UTC"));
    return Instant.now().atZone(zoneId);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other);
  }
}
