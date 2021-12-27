package org.folio.des.scheduling.base;

import org.folio.des.domain.dto.ScheduleParameters;

import java.util.Objects;

public abstract class AbstractExportTaskTrigger implements ExportTaskTrigger {
  protected ScheduleParameters scheduleParameters;

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
    return this.scheduleParameters.getId().equals(trigger.getScheduleParameters().getId());
  }
}
