package org.folio.des.scheduling.acquisition;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.base.AbstractExportTaskTrigger;
import org.springframework.scheduling.TriggerContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class AcqBaseExportTaskTrigger extends AbstractExportTaskTrigger {
  private final ScheduleParameters scheduleParameters;
  @Getter
  private final boolean enableScheduler;

  @Override
  public ScheduleParameters getScheduleParameters() {
    return this.scheduleParameters;
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
    return getNextTime(lastActualExecutionTime);
  }

  @Override
  public boolean isDisabledSchedule() {
    return Optional.ofNullable(scheduleParameters)
                  .map(ScheduleParameters::getSchedulePeriod)
                  .map(ScheduleParameters.SchedulePeriodEnum.NONE::equals)
                  .orElse(false) || enableScheduler;
  }

  protected Date getNextTime(Date lastActualExecutionTime) {
    if (scheduleParameters == null) return null;

    ScheduleParameters.SchedulePeriodEnum schedulePeriod = scheduleParameters.getSchedulePeriod();
    if (schedulePeriod == null || schedulePeriod == ScheduleParameters.SchedulePeriodEnum.NONE) return null;

    Date nextExecutionTime = new Date();
//    nextExecutionTime.toInstant().plusSeconds(scheduleParameters.getScheduleFrequency());
    Integer scheduleFrequency = scheduleParameters.getScheduleFrequency();

    switch (schedulePeriod) {
    case DAY:
      nextExecutionTime = scheduleTaskWithDayPeriod(lastActualExecutionTime, scheduleFrequency);
      break;
    case HOUR:
      nextExecutionTime = scheduleTaskWithHourPeriod(lastActualExecutionTime, scheduleFrequency);
      break;
    case WEEK:
      nextExecutionTime = scheduleTaskWeekly(lastActualExecutionTime, scheduleFrequency);
      break;
    default:
      return null;
    }

    return nextExecutionTime;
  }

  @SneakyThrows
  private Date scheduleTaskWeekly(Date lastActualExecutionTime, Integer everyWeek) {
    ZonedDateTime startTime = convertScheduleTimeToUTC(lastActualExecutionTime, scheduleParameters.getScheduleTime());
    if (lastActualExecutionTime == null) {
      everyWeek = 0;
    }
    startTime = findNextDayOfWeek(startTime, everyWeek);
    return convertToOldDateFormat(startTime);
  }

  private ZonedDateTime findNextDayOfWeek(ZonedDateTime initZoneDateTime, Integer everyWeek) {
    List<DayOfWeek> sortedDays = normalizeAndSortDayOfWeek();
    DayOfWeek firstDayOnTheWeek = sortedDays.stream().findFirst().get();
    Iterator<DayOfWeek> dayOfWeekIterator = sortedDays.iterator();
    DayOfWeek nextDayOfWeek = firstDayOnTheWeek;
    while (dayOfWeekIterator.hasNext()) {
      DayOfWeek dayOfWeek = dayOfWeekIterator.next();
      if (dayOfWeek.getValue() == initZoneDateTime.getDayOfWeek().getValue() && dayOfWeekIterator.hasNext()) {
        nextDayOfWeek = dayOfWeekIterator.next();
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

  private List<DayOfWeek> normalizeAndSortDayOfWeek() {
    List<ScheduleParameters.WeekDaysEnum> weekDays = scheduleParameters.getWeekDays();
    return weekDays.stream()
            .sorted(Comparator.comparing(ScheduleParameters.WeekDaysEnum::getValue))
            .map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString())).sorted().collect(Collectors.toList());
  }

  @SneakyThrows
  private Date scheduleTaskWithHourPeriod(Date lastActualExecutionTime, Integer hours) {
    ZonedDateTime startTimeUTC = convertScheduleTimeToUTC(lastActualExecutionTime, scheduleParameters.getScheduleTime());
    if (lastActualExecutionTime != null) {
      startTimeUTC = startTimeUTC.plusHours(hours);
    }
    return convertToOldDateFormat(startTimeUTC);
  }

  @SneakyThrows
  private Date scheduleTaskWithDayPeriod(Date lastActualExecutionTime, Integer days) {
    ZonedDateTime startTimeUTC = convertScheduleTimeToUTC(lastActualExecutionTime, scheduleParameters.getScheduleTime());
    if (lastActualExecutionTime != null) {
      startTimeUTC = startTimeUTC.plusDays(days);
    }
    return convertToOldDateFormat(startTimeUTC);
  }

  private Date convertToOldDateFormat(ZonedDateTime startTime) throws ParseException {
    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    isoFormat.setTimeZone(TimeZone.getTimeZone(scheduleParameters.getTimeZone()));
    return isoFormat.parse(startTime.truncatedTo(ChronoUnit.SECONDS).toString());
  }

  private ZonedDateTime convertScheduleTimeToUTC(Date lastActualExecutionTime, String scheduleTime) {
    ZoneId zoneId = ZoneId.of("UTC");
    ZonedDateTime startZoneDate = Instant.now().atZone(zoneId);
    if (lastActualExecutionTime != null) {
      Instant instant = Instant.ofEpochMilli(lastActualExecutionTime.getTime());
      startZoneDate = ZonedDateTime.ofInstant(instant, zoneId);
    } else {
      if (StringUtils.isNotEmpty(scheduleTime)) {
        LocalDate nowDate = startZoneDate.toLocalDate();
        LocalTime localTime = LocalTime.parse(scheduleTime, DateTimeFormatter.ISO_LOCAL_TIME);
        zoneId =  ZoneId.of(scheduleParameters.getTimeZone());
        return nowDate.atTime(localTime).atZone(zoneId).truncatedTo(ChronoUnit.SECONDS);
      }
    }
    return startZoneDate.truncatedTo(ChronoUnit.SECONDS);
  }
}
