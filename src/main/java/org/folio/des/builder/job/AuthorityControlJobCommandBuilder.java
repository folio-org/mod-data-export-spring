package org.folio.des.builder.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthorityControlJobCommandBuilder implements JobCommandBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobParameters buildJobCommand(Job job) {
    var parametersBuilder = new JobParametersBuilder();
    try {
      addJobCommand(parametersBuilder, "authorityControlExportConfig",
        objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getAuthorityControlExportConfig()));

      return parametersBuilder.toJobParameters();
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
