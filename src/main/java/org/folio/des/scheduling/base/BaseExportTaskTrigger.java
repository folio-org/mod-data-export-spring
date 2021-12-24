package org.folio.des.scheduling.base;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.ExportTrigger;
import org.springframework.scheduling.TriggerContext;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BaseExportTaskTrigger extends AbstractExportTaskTrigger implements ExportTaskTrigger {
  private final Set<String> schedulePeriodEnumSet = EnumSet.allOf(ScheduleParameters.SchedulePeriodEnum.class).stream()
    .map(ScheduleParameters.SchedulePeriodEnum::getValue).collect(Collectors.toSet());
  private final Set<String> weekDaysEnumSet = EnumSet.allOf(ScheduleParameters.WeekDaysEnum.class).stream()
    .map(ScheduleParameters.WeekDaysEnum::getValue).collect(Collectors.toSet());

  private final ExportTrigger exportTrigger;
  private final ScheduleParameters scheduleParameters;

  public BaseExportTaskTrigger(ExportTrigger exportTrigger) {
    ExportConfig exportConfig = exportTrigger.getConfig();
    scheduleParameters = buildScheduleParameters(exportConfig);
    this.exportTrigger = exportTrigger;
  }

  private ScheduleParameters buildScheduleParameters(ExportConfig exportConfig) {
    ScheduleParameters scheduleParam = null;
    if (exportConfig != null) {
      scheduleParam = new ScheduleParameters();
      scheduleParam.setId(UUID.fromString(exportConfig.getId()));
      scheduleParam.setScheduleFrequency(exportConfig.getScheduleFrequency());
      Optional<String> schedulePeriod = schedulePeriodEnumSet.stream()
                        .filter(period -> period.equals(exportConfig.getSchedulePeriod().getValue())).findAny();
      if (schedulePeriod.isPresent()) {
        ScheduleParameters.SchedulePeriodEnum period = ScheduleParameters.SchedulePeriodEnum.valueOf(schedulePeriod.get());
        scheduleParam.setSchedulePeriod(period);
      } else {
        scheduleParam.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
      }
      List<ExportConfig.WeekDaysEnum> weekDaysEnums = Optional.ofNullable(exportConfig.getWeekDays()).orElse(Collections.emptyList());
      Set<String> sourceWeekDays = weekDaysEnums.stream().map(ExportConfig.WeekDaysEnum::getValue).collect(Collectors.toSet());
      List<ScheduleParameters.WeekDaysEnum> weekDays = sourceWeekDays.stream()
        .filter(weekDaysEnumSet::contains)
        .map(ScheduleParameters.WeekDaysEnum::valueOf)
        .collect(Collectors.toList());

      scheduleParam.setScheduleTime(exportConfig.getScheduleTime());
      scheduleParam.setWeekDays(weekDays);
    }
    return scheduleParam;
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    return exportTrigger.nextExecutionTime(triggerContext);
  }

  @Override
  public ScheduleParameters getScheduleParameters() {
    return scheduleParameters;
  }

  @Override
  public boolean isDisabledSchedule() {
    return Optional.ofNullable(scheduleParameters)
                   .map(ScheduleParameters::getSchedulePeriod)
                   .map(ScheduleParameters.SchedulePeriodEnum.NONE::equals)
                   .orElse(false);
  }
}
