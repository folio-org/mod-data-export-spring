package org.folio.des.scheduling.acquisition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.folio.des.domain.dto.ScheduleParameters;
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
  private Date scheduleTaskWeekly(Date lastActualExecutionTime, Integer everyWeek) {
    ZonedDateTime startTime = ScheduleDateTimeUtil.convertScheduleTime(lastActualExecutionTime, scheduleParameters);
    if (lastActualExecutionTime == null) {
      everyWeek = 0;
    }
    startTime = findNextDayOfWeek(startTime, everyWeek);
    log.info("Weekly next schedule execution time in UTC for config {} is : {}", scheduleParameters.getId(), startTime);
    return ScheduleDateTimeUtil.convertToOldDateFormat(startTime, scheduleParameters);
  }

  private ZonedDateTime findNextDayOfWeek(ZonedDateTime initZoneDateTime, Integer everyWeek) {
    List<DayOfWeek> sortedDays = normalizeAndSortDayOfWeek();
    if (!sortedDays.isEmpty() && sortedDays.get(0) != null) {
      DayOfWeek firstDayOnTheWeek = sortedDays.get(0);
      Iterator<DayOfWeek> dayOfWeekIterator = sortedDays.iterator();
      DayOfWeek nextDayOfWeek = firstDayOnTheWeek;
      while (dayOfWeekIterator.hasNext()) {
        DayOfWeek dayOfWeek = dayOfWeekIterator.next();
        if (dayOfWeek.getValue() > initZoneDateTime.getDayOfWeek().getValue()) {
          nextDayOfWeek = dayOfWeek;
          break;
        }
      }
      int plusDays = 0;
      int nextDayOfWeekVal = nextDayOfWeek.getValue();
      if (nextDayOfWeekVal == firstDayOnTheWeek.getValue()) {
        plusDays = DayOfWeek.SUNDAY.getValue() - initZoneDateTime.getDayOfWeek().getValue() + 1;
        return initZoneDateTime.plusDays(plusDays).plusWeeks(everyWeek);
      }
      plusDays = nextDayOfWeekVal - initZoneDateTime.getDayOfWeek().getValue();
      return initZoneDateTime.plusDays(plusDays);
    }
    return null;
  }

  private List<DayOfWeek> normalizeAndSortDayOfWeek() {
    List<ScheduleParameters.WeekDaysEnum> weekDays = scheduleParameters.getWeekDays();
    return weekDays.stream()
            .sorted(Comparator.comparing(ScheduleParameters.WeekDaysEnum::getValue))
            .map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString())).sorted().collect(Collectors.toList());
  }

  @SneakyThrows
  private Date scheduleTaskWithHourPeriod(Date lastActualExecutionTime, Integer hours) {
    ZonedDateTime startTimeUTC = ScheduleDateTimeUtil.convertScheduleTime(lastActualExecutionTime, scheduleParameters);
    ZoneId zoneId = ZoneId.of("UTC");
    ZonedDateTime nowDate = Instant.now().atZone(zoneId);
    if (lastActualExecutionTime != null) {
      long diffHours = (nowDate.toInstant().toEpochMilli() - startTimeUTC.toInstant().toEpochMilli())/(60 * 60 * 1000);
      if (diffHours > 0 && hours !=0 && diffHours > hours) {
        BigDecimal hoursToIncrease = BigDecimal.valueOf(diffHours)
          .divide(BigDecimal.valueOf(hours), RoundingMode.FLOOR)
          .add(BigDecimal.ONE);
        startTimeUTC = startTimeUTC.plusHours(hoursToIncrease.longValue() * hours);
      } else
      {
        startTimeUTC = startTimeUTC.plusHours(hours);
      }
    }
    log.info("Hourly next schedule execution time in UTC for config {} is : {}", scheduleParameters.getId(), startTimeUTC);
    return ScheduleDateTimeUtil.convertToOldDateFormat(startTimeUTC, scheduleParameters);
  }

  @SneakyThrows
  private Date scheduleTaskWithDayPeriod(Date lastActualExecutionTime, Integer days) {
    ZonedDateTime startTimeUTC = ScheduleDateTimeUtil.convertScheduleTime(lastActualExecutionTime, scheduleParameters);
    if (lastActualExecutionTime != null) {
      startTimeUTC = startTimeUTC.plusDays(days);
    }
    log.info("Day next schedule execution time in UTC for config {} is : {}", scheduleParameters.getId(), startTimeUTC);
    return ScheduleDateTimeUtil.convertToOldDateFormat(startTimeUTC, scheduleParameters);
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
