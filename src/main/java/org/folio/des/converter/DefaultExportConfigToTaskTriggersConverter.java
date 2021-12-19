package org.folio.des.converter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.scheduling.ExportTaskTrigger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Log4j2
@Service
public class DefaultExportConfigToTaskTriggersConverter implements Converter<ExportConfig, List<ExportTaskTrigger>>  {
  private final Set<String> schedulePeriodEnumSet = EnumSet.allOf(ScheduleParameters.SchedulePeriodEnum.class).stream()
                                                  .map(ScheduleParameters.SchedulePeriodEnum::getValue).collect(Collectors.toSet());
  private final Set<String> weekDaysEnumSet = EnumSet.allOf(ScheduleParameters.WeekDaysEnum.class).stream()
                                                  .map(ScheduleParameters.WeekDaysEnum::getValue).collect(Collectors.toSet());

  @Override
  public List<ExportTaskTrigger> convert(ExportConfig exportConfig) {
    if (ExportConfig.SchedulePeriodEnum.NONE != exportConfig.getSchedulePeriod()) {
      ScheduleParameters scheduleParameters = new ScheduleParameters();
      scheduleParameters.setId(UUID.fromString(exportConfig.getId()));
      scheduleParameters.setScheduleFrequency(exportConfig.getScheduleFrequency());
      schedulePeriodEnumSet.stream().filter(period -> period.equals(exportConfig.getSchedulePeriod().getValue())).findAny().ifPresent(period -> scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(period)));

      List<ExportConfig.WeekDaysEnum> weekDaysEnums = Optional.ofNullable(exportConfig.getWeekDays()).orElse(Collections.emptyList());
      Set<String> sourceWeekDays = weekDaysEnums.stream().map(ExportConfig.WeekDaysEnum::getValue).collect(Collectors.toSet());
      List<ScheduleParameters.WeekDaysEnum> weekDays = sourceWeekDays.stream()
        .filter(weekDaysEnumSet::contains)
        .map(ScheduleParameters.WeekDaysEnum::valueOf)
        .collect(Collectors.toList());

      scheduleParameters.setScheduleTime(exportConfig.getScheduleTime());
      scheduleParameters.setWeekDays(weekDays);
      return List.of(new ExportTaskTrigger(scheduleParameters));
    }
    return Collections.emptyList();
  }
}
