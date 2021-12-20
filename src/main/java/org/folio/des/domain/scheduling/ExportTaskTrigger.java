package org.folio.des.domain.scheduling;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.ScheduleParameters;
import org.springframework.scheduling.Trigger;

import lombok.Getter;

@RequiredArgsConstructor
public abstract class ExportTaskTrigger implements Trigger {
  @Getter
  protected final ScheduleParameters scheduleParameters;

}
