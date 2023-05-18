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
import java.time.OffsetTime;
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
      Calendar cal1 =new GregorianCalendar();
      SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
      scheduleParameters.setScheduleTime(dateFormat.format(cal1.getTime()));
      log.info("HOUR Unit with time:{} ",dateFormat.format(cal1.getTime()).toString());
    }
    else {
      scheduleParameters.setScheduleTime( OffsetTime.parse(exportConfig.getScheduleTime()).toLocalTime().toString());
    }
    scheduleParameters.setScheduleFrequency(exportConfig.getScheduleFrequency());
    scheduleParameters.setTimeZone(timeZone);
    if(exportConfig.getSchedulePeriod().equals(ExportConfig.SchedulePeriodEnum.WEEK)) {
      scheduleParameters.setWeekDays(createWeekDaysEnum(exportConfig.getWeekDays()));
    }
    else {
      scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(exportConfig.getSchedulePeriod().name()));
    }
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
