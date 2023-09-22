package org.folio.des.scheduling.quartz.trigger;

import java.util.Set;

import org.quartz.Trigger;

public record ExportTrigger(boolean isDisabled, Set<Trigger> triggers) {
}
