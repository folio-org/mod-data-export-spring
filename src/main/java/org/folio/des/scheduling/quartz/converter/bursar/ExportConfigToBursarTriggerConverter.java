package org.folio.des.scheduling.quartz.converter.bursar;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverter;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ExportConfigToBursarTriggerConverter implements Converter<ExportConfig, ExportTrigger> {

  private final ScheduleParametersToTriggerConverter scheduleParametersToTriggerConverter;
  private final String timeZone;

  public ExportConfigToBursarTriggerConverter(ScheduleParametersToTriggerConverter scheduleParametersToTriggerConverter,
                                              @Value("${folio.quartz.bursar.timeZone}") String timeZone) {
    this.scheduleParametersToTriggerConverter = scheduleParametersToTriggerConverter;
    this.timeZone = timeZone;
  }

  @Override
  public ExportTrigger convert(@NotNull ExportConfig exportConfig) {

    if (isDisabledSchedule(exportConfig)) {
      return new ExportTrigger(true, Collections.emptySet());
    }
    ScheduleParameters scheduleParameters = createBursarScheduleParameters(exportConfig);

    return new ExportTrigger(false, scheduleParametersToTriggerConverter
      .convert(scheduleParameters, getTriggerGroup(exportConfig)));
  }

  private boolean isDisabledSchedule(ExportConfig exportConfig) {
    return Optional.ofNullable(exportConfig.getSchedulePeriod())
      .map(ExportConfig.SchedulePeriodEnum.NONE::equals)
      .orElse(true);
  }

  private ScheduleParameters createBursarScheduleParameters(ExportConfig exportConfig) {

    ExportConfig.SchedulePeriodEnum schedulePeriod = exportConfig.getSchedulePeriod();

    return switch (schedulePeriod) {
      case HOUR -> buildHourlyScheduleParam(exportConfig);
      case DAY -> buildDailyScheduleParam(exportConfig);
      case WEEK -> buildWeeklyScheduleParam(exportConfig);
      default -> null;
    };
  }

  private ScheduleParameters buildWeeklyScheduleParam(ExportConfig exportConfig) {
    log.info("buildWeeklyScheduleParam:: configId:{}", exportConfig.getId());
    return buildCommonScheduleParam(exportConfig)
      .scheduleTime(OffsetTime.parse(exportConfig.getScheduleTime()).toLocalTime().toString())
      .weekDays(createWeekDaysEnum(exportConfig.getWeekDays()));
  }

  private ScheduleParameters buildDailyScheduleParam(ExportConfig exportConfig) {
    log.info("buildDailyScheduleParam:: configId:{}", exportConfig.getId());
    return buildCommonScheduleParam(exportConfig)
      .scheduleTime(OffsetTime.parse(exportConfig.getScheduleTime()).toLocalTime().toString());
  }

  private ScheduleParameters buildHourlyScheduleParam(ExportConfig exportConfig) {
    log.info("buildHourlyScheduleParam:: configId:{}", exportConfig.getId());
    return buildCommonScheduleParam(exportConfig)
      .scheduleTime(calculateHourlyScheduledTime());
  }

  private ScheduleParameters buildCommonScheduleParam(ExportConfig exportConfig) {
    return new ScheduleParameters()
      .scheduleFrequency(exportConfig.getScheduleFrequency())
      .timeZone(timeZone)
      .schedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(exportConfig.getSchedulePeriod().name()));
  }

  private String calculateHourlyScheduledTime() {
    final ZonedDateTime nextHour = ZonedDateTime.now(ZoneId.of(timeZone)).plusHours(1L);
    String format = DateTimeFormatter.ofPattern("HH:mm:ss").format(nextHour);
    return LocalDate.now().format(DateTimeFormatter.ofPattern(format));
  }

  private List<ScheduleParameters.WeekDaysEnum> createWeekDaysEnum(List<ExportConfig.WeekDaysEnum> weekDays) {
    List<ScheduleParameters.WeekDaysEnum> weekDaysEnum = new ArrayList<>();
    weekDays.forEach(exportConfigWeekDaysEnum ->
      weekDaysEnum.add(ScheduleParameters.WeekDaysEnum.valueOf(exportConfigWeekDaysEnum.name())));

    return weekDaysEnum;
  }

  private String getTriggerGroup(ExportConfig exportConfig) {
    return exportConfig.getTenant() + "_" + QuartzConstants.BURSAR_EXPORT_GROUP_NAME;
  }
}
