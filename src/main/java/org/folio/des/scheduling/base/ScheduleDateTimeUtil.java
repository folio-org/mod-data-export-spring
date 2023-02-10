package org.folio.des.scheduling.base;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ScheduleParameters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

public final class ScheduleDateTimeUtil {

  private ScheduleDateTimeUtil() {

  }
  public static Instant convertToOldDateFormat(ZonedDateTime startTime, ScheduleParameters scheduleParameters) throws ParseException {
    if (startTime == null) {
      throw new ParseException("Start time can not be empty", 0);
    }
    String format = "yyyy-MM-dd'T'HH:mm:ssX";

    if (startTime.getSecond() == 0) {
      format = "yyyy-MM-dd'T'HH:mmX";
    }

    SimpleDateFormat isoFormat = new SimpleDateFormat(format);
    isoFormat.setTimeZone(TimeZone.getTimeZone(scheduleParameters.getTimeZone()));
    return isoFormat.parse(startTime.truncatedTo(ChronoUnit.SECONDS).toString()).toInstant();
  }

  public static ZonedDateTime convertScheduleTime(Instant lastActualExecutionTime, ScheduleParameters scheduleParameters) {
    if (lastActualExecutionTime != null) {
      ZoneId zoneId = ZoneId.of("UTC");
      return ZonedDateTime.ofInstant(lastActualExecutionTime, zoneId).truncatedTo(ChronoUnit.SECONDS);

    } else if (StringUtils.isNotEmpty(scheduleParameters.getScheduleTime())) {
      LocalTime localTime = LocalTime.parse(scheduleParameters.getScheduleTime(), DateTimeFormatter.ISO_LOCAL_TIME);
      ZoneId zoneId =  ZoneId.of(scheduleParameters.getTimeZone());
      ZonedDateTime startZoneDate = Instant.now().atZone(zoneId);
      LocalDate nowDate = startZoneDate.toLocalDate();
      return nowDate.atTime(localTime).atZone(zoneId).truncatedTo(ChronoUnit.SECONDS);

    } else {
      return getUtcDateTime().truncatedTo(ChronoUnit.SECONDS);
    }
  }

  private static ZonedDateTime getUtcDateTime() {
    ZoneId zoneId = ZoneId.of("UTC");
    return Instant.now().atZone(zoneId);
  }
}
