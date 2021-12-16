package org.folio.des.converter.scheduling;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.scheduling.ExportTaskTrigger;
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
  public List<ExportTaskTrigger> convert(ExportConfig source) {
    if (ExportConfig.SchedulePeriodEnum.NONE != source.getSchedulePeriod()) {
      ScheduleParameters scheduleParameters = new ScheduleParameters();
      scheduleParameters.setScheduleFrequency(source.getScheduleFrequency());
      schedulePeriodEnumSet.stream().filter(period -> period.equals(source.getSchedulePeriod().getValue())).findAny().ifPresent(period -> scheduleParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.valueOf(period)));

      Set<String> sourceWeekDays = source.getWeekDays().stream().map(ExportConfig.WeekDaysEnum::getValue).collect(Collectors.toSet());
      List<ScheduleParameters.WeekDaysEnum> weekDays = sourceWeekDays.stream()
        .filter(weekDaysEnumSet::contains)
        .map(ScheduleParameters.WeekDaysEnum::valueOf)
        .collect(Collectors.toList());

      scheduleParameters.setScheduleTime(source.getScheduleTime());
      scheduleParameters.setWeekDays(weekDays);
      return List.of(new ExportTaskTrigger(source.getId(), scheduleParameters));
    }
    return Collections.emptyList();
  }
}
