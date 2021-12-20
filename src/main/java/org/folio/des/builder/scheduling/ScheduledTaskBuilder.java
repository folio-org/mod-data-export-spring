package org.folio.des.builder.scheduling;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.scheduling.ScheduledTask;

import java.util.Optional;

@FunctionalInterface
public interface ScheduledTaskBuilder {
  Optional<ScheduledTask> buildTask(ExportConfig exportConfig);
}
