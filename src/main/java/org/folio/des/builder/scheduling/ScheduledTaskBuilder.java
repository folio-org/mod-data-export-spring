package org.folio.des.builder.scheduling;

import org.folio.des.domain.dto.ExportConfig;

@FunctionalInterface
public interface ScheduledTaskBuilder {
  Runnable buildTask(ExportConfig exportConfig);
}
