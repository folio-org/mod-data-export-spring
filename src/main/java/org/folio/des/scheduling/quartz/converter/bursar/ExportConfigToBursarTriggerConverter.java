package org.folio.des.scheduling.quartz.converter.bursar;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverter;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Log4j2
@RequiredArgsConstructor
public class ExportConfigToBursarTriggerConverter implements Converter<ExportConfig, ExportTrigger> {

  private final ScheduleParametersToTriggerConverter scheduleParametersToTriggerConverter;

  @Value("${folio.quartz.bursar.timeZone}")
  private String timeZone;

  @Override
  public ExportTrigger convert(@NotNull ExportConfig exportConfig) {

    ScheduleParameters scheduleParameters = createBursarScheduleParameters(exportConfig);

    return new ExportTrigger(false, scheduleParametersToTriggerConverter
      .convert(scheduleParameters, getTriggerGroup(exportConfig)));
  }

  private ScheduleParameters createBursarScheduleParameters(ExportConfig exportConfig) {
    ScheduleParameters scheduleParameters = new ScheduleParameters();

    if(exportConfig.getScheduleTime()==null || exportConfig.getScheduleTime().isEmpty()) {
      final ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timeZone));
      String format = DateTimeFormatter.ofPattern("HH:mm:ss").format(now);
      String today = LocalDate.now().format(DateTimeFormatter.ofPattern(format));
      scheduleParameters.setScheduleTime(today);
      log.info("HOUR Unit with time:{} ",today);
    }
    else {
      scheduleParameters.setScheduleTime( OffsetTime.parse(exportConfig.getScheduleTime()).toLocalTime().toString());
    }
    scheduleParameters.setScheduleFrequency(exportConfig.getScheduleFrequency());
    scheduleParameters.setTimeZone(timeZone);
    if(exportConfig.getSchedulePeriod().equals(ExportConfig.SchedulePeriodEnum.WEEK)) {
      scheduleParameters.setWeekDays(createWeekDaysEnum(exportConfig.getWeekDays()));
    }
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(exportConfig.getSchedulePeriod().name()));

    return scheduleParameters;
  }

  private List<ScheduleParameters.WeekDaysEnum> createWeekDaysEnum(List<ExportConfig.WeekDaysEnum> weekDays) {

    List<ScheduleParameters.WeekDaysEnum> weekDaysEnums = new ArrayList<>();
    weekDays.forEach(exportConfigWeekDaysEnum ->
     weekDaysEnums.add(ScheduleParameters.WeekDaysEnum.valueOf(exportConfigWeekDaysEnum.name())));

    return weekDaysEnums;
  }

  private String getTriggerGroup(ExportConfig exportConfig) {
    return exportConfig.getTenant() + "_" + QuartzConstants.BURSAR_EXPORT_GROUP_NAME;
  }
}
