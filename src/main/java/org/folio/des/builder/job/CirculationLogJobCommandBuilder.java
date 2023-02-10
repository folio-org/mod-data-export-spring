package org.folio.des.builder.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationLogJobCommandBuilder implements JobCommandBuilder {
  @Override
  public JobParameters buildJobCommand(Job job) {
    var parametersBuilder = new JobParametersBuilder();
    addJobCommand(parametersBuilder, "query", job.getExportTypeSpecificParameters().getQuery());
    return parametersBuilder.toJobParameters();
  }
}
