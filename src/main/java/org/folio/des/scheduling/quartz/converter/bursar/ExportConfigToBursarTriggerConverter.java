package org.folio.des.scheduling.quartz.converter.bursar;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverter;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

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

    ExportConfig.SchedulePeriodEnum schedulePeriod = exportConfig.getSchedulePeriod();

    if (ExportConfig.SchedulePeriodEnum.NONE.equals(schedulePeriod)) return null;

    return switch (schedulePeriod) {
      case HOUR -> buildHourlyScheduleParam(exportConfig);
      case DAY -> buildDailyScheduleParam(exportConfig);
      case WEEK -> buildWeeklyScheduleParam(exportConfig);
      default -> null;
    };
  }

  private ScheduleParameters buildWeeklyScheduleParam(ExportConfig exportConfig) {
    log.info("Inside buildWeeklyScheduleParam");
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setScheduleFrequency(exportConfig.getScheduleFrequency());
    scheduleParameters.setTimeZone(timeZone);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(exportConfig.getSchedulePeriod().name()));
    scheduleParameters.setScheduleTime(OffsetTime.parse(exportConfig.getScheduleTime()).toLocalTime().toString());
    scheduleParameters.setWeekDays(createWeekDaysEnum(exportConfig.getWeekDays()));

    return scheduleParameters;
  }

  private ScheduleParameters buildDailyScheduleParam(ExportConfig exportConfig) {
    log.info("Inside buildDailyScheduleParam");
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setScheduleFrequency(exportConfig.getScheduleFrequency());
    scheduleParameters.setTimeZone(timeZone);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(exportConfig.getSchedulePeriod().name()));
    scheduleParameters.setScheduleTime(OffsetTime.parse(exportConfig.getScheduleTime()).toLocalTime().toString());
    return scheduleParameters;
  }

  private ScheduleParameters buildHourlyScheduleParam(ExportConfig exportConfig) {
    log.info("Inside buildHourlyScheduleParam");
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setScheduleFrequency(exportConfig.getScheduleFrequency());
    scheduleParameters.setTimeZone(timeZone);
    scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(exportConfig.getSchedulePeriod().name()));
    setHourlyScheduledTime(scheduleParameters);

    return scheduleParameters;
  }

  private void setHourlyScheduledTime(ScheduleParameters scheduleParameters) {
    final ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timeZone));
    String format = DateTimeFormatter.ofPattern("HH:mm:ss").format(now);
    scheduleParameters.setScheduleTime(LocalDate.now().format(DateTimeFormatter.ofPattern(format)));
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
