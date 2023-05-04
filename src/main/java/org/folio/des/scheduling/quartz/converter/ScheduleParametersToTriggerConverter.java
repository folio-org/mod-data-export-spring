package org.folio.des.scheduling.quartz.converter;

import java.util.Set;

import org.folio.des.domain.dto.ScheduleParameters;
import org.quartz.Trigger;

public interface ScheduleParametersToTriggerConverter {
  Set<Trigger> convert(ScheduleParameters scheduleParameters, String triggerGroup);
}
