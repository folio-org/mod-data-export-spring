package org.folio.des.scheduling.base;

import org.folio.des.domain.dto.ScheduleParameters;
import org.springframework.scheduling.Trigger;


public interface ExportTaskTrigger extends Trigger {

  ScheduleParameters getScheduleParameters();
  boolean isDisabledSchedule();
}
