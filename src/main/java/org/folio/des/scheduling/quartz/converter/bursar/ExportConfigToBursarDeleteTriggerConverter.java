package org.folio.des.scheduling.quartz.converter.bursar;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
public class ExportConfigToBursarDeleteTriggerConverter implements Converter<ExportConfig, ExportTrigger> {

  private final ScheduleParametersToTriggerConverter scheduleParametersToTriggerConverter;
  private final String timeZone;

  public ExportConfigToBursarDeleteTriggerConverter(ScheduleParametersToTriggerConverter scheduleParametersToTriggerConverter,
                                                    @Value("${folio.quartz.bursar.timeZone}") String timeZone) {
    this.scheduleParametersToTriggerConverter = scheduleParametersToTriggerConverter;
    this.timeZone = timeZone;
  }

  @Override
  public ExportTrigger convert(@NotNull ExportConfig exportConfig) {

    ScheduleParameters scheduleParameters = createBursarDeleteScheduleParameters();

    return new ExportTrigger(false, scheduleParametersToTriggerConverter
      .convert(scheduleParameters, getTriggerGroup(exportConfig)));
  }

  private ScheduleParameters createBursarDeleteScheduleParameters() {
    return new ScheduleParameters()
      .scheduleFrequency(1)
      .timeZone(timeZone)
      .scheduleTime(calculateHourlyScheduledTime())
      .schedulePeriod(ScheduleParameters.SchedulePeriodEnum.DAY);
  }

  private String calculateHourlyScheduledTime() {
    final ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timeZone));
    String format = DateTimeFormatter.ofPattern("HH:mm:ss").format(now);
    return LocalDate.now(ZoneId.of(timeZone)).format(DateTimeFormatter.ofPattern(format));
  }

  private String getTriggerGroup(ExportConfig exportConfig) {
    return exportConfig.getTenant() + "_" + QuartzConstants.BURSAR_EXPORT_DELETE_GROUP_NAME;
  }
}
