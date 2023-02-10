package org.folio.des.builder.job;

import org.folio.de.entity.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.util.Optional;

@FunctionalInterface
public interface JobCommandBuilder {
  JobParameters buildJobCommand(Job job);

  default void addJobCommand(JobParametersBuilder jobParametersBuilder, String paramKey, String jobParam) {
    Optional.ofNullable(jobParam)
      .ifPresent(param -> jobParametersBuilder.addString(paramKey, param));
  }
}
