package org.folio.des.scheduling.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ScheduleParameters;

public final class ScheduleDateTimeUtil {

  private ScheduleDateTimeUtil() {

  }

  public static Date convertScheduleTimeToDate(ScheduleParameters scheduleParameters) {
    if (StringUtils.isNotEmpty(scheduleParameters.getScheduleTime())) {
      LocalTime localTime = LocalTime.parse(scheduleParameters.getScheduleTime(), DateTimeFormatter.ISO_LOCAL_TIME);
      ZoneId zoneId = ZoneId.of(scheduleParameters.getTimeZone());
      ZonedDateTime startZoneDate = Instant.now().atZone(zoneId);
      LocalDate nowDate = startZoneDate.toLocalDate();
      return Date.from(nowDate.atTime(localTime).atZone(zoneId).truncatedTo(ChronoUnit.SECONDS).toInstant());
    } else {
      return Date.from(getUtcDateTime().truncatedTo(ChronoUnit.SECONDS).toInstant());
    }
  }

  public static Date convertScheduleTimeForWeekDayToDate(ScheduleParameters scheduleParameters, DayOfWeek dayOfWeek) {
    if (StringUtils.isNotEmpty(scheduleParameters.getScheduleTime())) {
      LocalTime localTime = LocalTime.parse(scheduleParameters.getScheduleTime(), DateTimeFormatter.ISO_LOCAL_TIME);
      ZoneId zoneId = ZoneId.of(scheduleParameters.getTimeZone());
      LocalDate localDate = LocalDate.now(zoneId).with(TemporalAdjusters.nextOrSame(dayOfWeek));
      ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, zoneId);
      return Date.from(zonedDateTime.toInstant());
    } else {
      return Date.from(getUtcDateTime().with(TemporalAdjusters.nextOrSame(dayOfWeek))
        .truncatedTo(ChronoUnit.SECONDS).toInstant());
    }
  }

  private static ZonedDateTime getUtcDateTime() {
    ZoneId zoneId = ZoneId.of("UTC");
    return Instant.now().atZone(zoneId);
  }
}
