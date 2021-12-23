package org.folio.des.domain.scheduling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.Job;

@RequiredArgsConstructor
public class ScheduledTask {
  @Getter
  private final Runnable task;
  @Getter
  private final Job job;
}
