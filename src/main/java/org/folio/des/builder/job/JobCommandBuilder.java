package org.folio.des.builder.job;

import org.folio.de.entity.Job;
import org.springframework.batch.core.job.parameters.JobParameters;

@FunctionalInterface
public interface JobCommandBuilder {
  JobParameters buildJobCommand(Job job);
}
