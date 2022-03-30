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
import java.util.Date;
import java.util.TimeZone;

public final class ScheduleDateTimeUtil {

  private ScheduleDateTimeUtil() {

  }
  public static Date convertToOldDateFormat(ZonedDateTime startTime, ScheduleParameters scheduleParameters) throws ParseException {
    if (startTime == null) {
      throw new ParseException("Start time can not be empty", 0);
    }
    String format = "yyyy-MM-dd'T'HH:mm:ssX";

    if (startTime.getSecond() == 0) {
      format = "yyyy-MM-dd'T'HH:mmX";
    }

    SimpleDateFormat isoFormat = new SimpleDateFormat(format);
    isoFormat.setTimeZone(TimeZone.getTimeZone(scheduleParameters.getTimeZone()));
    return isoFormat.parse(startTime.truncatedTo(ChronoUnit.SECONDS).toString());
  }

  public static ZonedDateTime convertScheduleTime(Date lastActualExecutionTime, ScheduleParameters scheduleParameters) {
    ZoneId zoneId = ZoneId.of("UTC");
    ZonedDateTime startZoneDate = Instant.now().atZone(zoneId);
    if (lastActualExecutionTime != null) {
      Instant instant = Instant.ofEpochMilli(lastActualExecutionTime.getTime());
      startZoneDate = ZonedDateTime.ofInstant(instant, zoneId);
    } else {
      if (StringUtils.isNotEmpty(scheduleParameters.getScheduleTime())) {
        LocalDate nowDate = startZoneDate.toLocalDate();
        LocalTime localTime = LocalTime.parse(scheduleParameters.getScheduleTime(), DateTimeFormatter.ISO_LOCAL_TIME);
        zoneId =  ZoneId.of(scheduleParameters.getTimeZone());
        return nowDate.atTime(localTime).atZone(zoneId).truncatedTo(ChronoUnit.SECONDS);
      }
    }
    return startZoneDate.truncatedTo(ChronoUnit.SECONDS);
  }
}
