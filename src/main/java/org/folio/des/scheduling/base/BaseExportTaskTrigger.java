package org.folio.des.scheduling.base;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
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
public class BaseExportTaskTrigger extends ExportTrigger implements ExportTaskTrigger {
  private final Set<String> schedulePeriodEnumSet = EnumSet.allOf(ScheduleParameters.SchedulePeriodEnum.class).stream()
    .map(ScheduleParameters.SchedulePeriodEnum::getValue).collect(Collectors.toSet());
  private final Set<String> weekDaysEnumSet = EnumSet.allOf(ScheduleParameters.WeekDaysEnum.class).stream()
    .map(ScheduleParameters.WeekDaysEnum::getValue).collect(Collectors.toSet());

  private final ScheduleParameters scheduleParameters;

  public BaseExportTaskTrigger(ExportTrigger exportTrigger) {
    this.setConfig(Optional.ofNullable(exportTrigger).map(ExportTrigger::getConfig).orElse(null));
    ExportConfig exportConfig = getConfig();
    scheduleParameters = buildScheduleParameters(exportConfig);
  }

  private ScheduleParameters buildScheduleParameters(ExportConfig exportConfig) {
    ScheduleParameters scheduleParam = null;
    if (exportConfig != null && exportConfig.getSchedulePeriod() != null) {
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
    Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
    return getNextTime(lastActualExecutionTime);
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

  @Override
  public int hashCode() {
    return Objects.hash(getScheduleParameters().getId());
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof ExportTaskTrigger)) {
      return false;
    }
    ExportTaskTrigger trigger = ((ExportTaskTrigger) other);
    return this.getScheduleParameters().getId().equals(trigger.getScheduleParameters().getId());
  }
}
