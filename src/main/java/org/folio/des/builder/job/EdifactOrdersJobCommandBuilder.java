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
public class EdifactOrdersJobCommandBuilder implements JobCommandBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobParameters buildJobCommand(Job job) {
    log.debug("Build job command by job={}", job);
    var paramsBuilder = new JobParametersBuilder();
    try {
      paramsBuilder.addString("edifactOrdersExport",
        objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig()));
      JobParameters jobParameters = paramsBuilder.toJobParameters();
      log.debug("Job params: {}", jobParameters);
      return jobParameters;
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
