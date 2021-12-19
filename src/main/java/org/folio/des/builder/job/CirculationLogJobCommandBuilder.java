package org.folio.des.builder.job;

import java.util.HashMap;
import java.util.Map;

import org.folio.de.entity.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationLogJobCommandBuilder implements JobCommandBuilder {
  @Override
  public JobParameters buildJobCommand(Job job) {
    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter(job.getExportTypeSpecificParameters().getQuery()));
    return new JobParameters(params);
  }
}
