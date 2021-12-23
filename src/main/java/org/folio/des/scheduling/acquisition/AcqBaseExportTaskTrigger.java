package org.folio.des.scheduling.acquisition;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.springframework.scheduling.TriggerContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
public class AcqBaseExportTaskTrigger implements ExportTaskTrigger {
  private final ScheduleParameters scheduleParameters;

  @Override
  public ScheduleParameters getScheduleParameters() {
    return this.scheduleParameters;
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
    return getNextTime(lastActualExecutionTime);
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
  private Date scheduleTaskWeekly(Date lastActualExecutionTime, Integer scheduleFrequency) {
    ZonedDateTime startTime = convertScheduleTimeToUTC(lastActualExecutionTime, scheduleParameters.getScheduleTime());
    if (lastActualExecutionTime != null) {
     // startTime = findNextDayOfWeek(startTime, scheduleFrequency);
    }
    return convertToOldDateFormat(startTime);
//    if (lastActualExecutionTime == null) {
//      return Date.from(startTime.toInstant());
//
//    } else {
//      var lastExecutionOffsetDateTime = OffsetDateTime.ofInstant(lastActualExecutionTime.toInstant(),
//        ZoneId.systemDefault());
//      var instant = findNextDayOfWeek(lastExecutionOffsetDateTime, scheduleFrequency).toInstant();
//      return Date.from(instant);
//    }
  }

//  private OffsetDateTime findNextDayOfWeek(ZonedDateTime zonedDateTime, Integer everyWeek) {
//    List<DayOfWeek> weeks = normalizeDayOfWeek();
//
//    var currentDayOfWeek = zonedDateTime.getDayOfWeek();
//    for (DayOfWeek ofWeek : weeks) {
//      int nextWeekDay = currentDayOfWeek.getValue() - ofWeek.getValue();
//      if (nextWeekDay >= 1) {
//        return zonedDateTime.plusDays(nextWeekDay);
//      }
//    }
//    int daysBefore = weeks.get(0).getValue() - currentDayOfWeek.getValue();
//
//    return zonedDateTime.minusDays(daysBefore).plusWeeks(everyWeek);
//  }

  private List<DayOfWeek> normalizeDayOfWeek() {
    List<ScheduleParameters.WeekDaysEnum> weekDays = scheduleParameters.getWeekDays();
    return weekDays.stream().map(weekDaysEnum -> DayOfWeek.valueOf(weekDaysEnum.toString())).sorted().collect(Collectors.toList());
  }

  @SneakyThrows
  private Date scheduleTaskWithHourPeriod(Date lastActualExecutionTime, Integer hours) {
    ZonedDateTime startTime = convertScheduleTimeToUTC(lastActualExecutionTime, scheduleParameters.getScheduleTime());
    if (lastActualExecutionTime != null) {
      startTime = startTime.plusHours(hours);
    }
    return convertToOldDateFormat(startTime);
  }

  @SneakyThrows
  private Date scheduleTaskWithDayPeriod(Date lastActualExecutionTime, Integer days) {
    ZonedDateTime startTime = convertScheduleTimeToUTC(lastActualExecutionTime, scheduleParameters.getScheduleTime());
    if (lastActualExecutionTime != null) {
      startTime = startTime.plusDays(days);
    }
    return convertToOldDateFormat(startTime);
  }

  private Date convertToOldDateFormat(ZonedDateTime startTime) throws ParseException {
    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z[UTC]'");
    isoFormat.setTimeZone(TimeZone.getTimeZone(scheduleParameters.getTimeZone()));
    return isoFormat.parse(startTime.truncatedTo(ChronoUnit.SECONDS).toString());
  }

  private ZonedDateTime convertScheduleTimeToUTC(Date lastActualExecutionTime, String scheduleTime) {
    ZoneId zoneId = ZoneId.of("UTC");
    ZonedDateTime startZoneDate = Instant.now().atZone(zoneId);
    if (lastActualExecutionTime != null) {
      Instant instant = Instant.ofEpochMilli(lastActualExecutionTime.getTime());
      startZoneDate = ZonedDateTime.ofInstant(instant, zoneId);
    }
    if (StringUtils.isNotEmpty(scheduleTime)) {
      LocalDate nowDate = startZoneDate.toLocalDate();
      LocalTime localTime = LocalTime.parse(scheduleTime, DateTimeFormatter.ISO_LOCAL_TIME);
      return nowDate.atTime(localTime).atZone(zoneId).truncatedTo(ChronoUnit.SECONDS);
    }
    return startZoneDate.truncatedTo(ChronoUnit.SECONDS);
  }
}
