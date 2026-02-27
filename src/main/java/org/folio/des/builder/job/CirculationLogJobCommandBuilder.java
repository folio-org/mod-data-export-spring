package org.folio.des.builder.job;

import org.folio.de.entity.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationLogJobCommandBuilder implements JobCommandBuilder {
  @Override
  public JobParameters buildJobCommand(Job job) {
    var paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString("query", job.getExportTypeSpecificParameters().getQuery());
    return paramsBuilder.toJobParameters();
  }
}
