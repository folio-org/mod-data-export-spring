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

import org.apache.commons.lang3.StringUtils;
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
