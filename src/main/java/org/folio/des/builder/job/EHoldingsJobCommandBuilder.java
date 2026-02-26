package org.folio.des.builder.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Log4j2
@RequiredArgsConstructor
public class EHoldingsJobCommandBuilder implements JobCommandBuilder {

  private final ObjectMapper objectMapper;

  @Override
  public JobParameters buildJobCommand(Job job) {
    var paramsBuilder = new JobParametersBuilder();
    try {
      paramsBuilder.addString("eHoldingsExportConfig",
        objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().geteHoldingsExportConfig()));
      return paramsBuilder.toJobParameters();
    } catch (JacksonException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
