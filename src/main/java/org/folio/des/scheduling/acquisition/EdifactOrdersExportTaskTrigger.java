package org.folio.des.scheduling.acquisition;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.springframework.scheduling.TriggerContext;

import java.util.Date;

@Log4j2
@RequiredArgsConstructor
public class EdifactOrdersExportTaskTrigger implements ExportTaskTrigger {
  private final ScheduleParameters scheduleParameters;

  @Override
  public ScheduleParameters getScheduleParameters() {
    return this.scheduleParameters;
  }

  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    return null;
  }
}
