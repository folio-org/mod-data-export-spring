package org.folio.des.builder.job;

import org.folio.de.entity.JobCommand;
import org.folio.des.domain.dto.Job;

@FunctionalInterface
public interface JobCommandSchedulerBuilder {
  JobCommand buildJobCommand(Job job);
}
