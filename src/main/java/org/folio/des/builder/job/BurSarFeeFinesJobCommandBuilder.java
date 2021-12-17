package org.folio.des.builder.job;

import java.util.HashMap;
import java.util.Map;

import org.folio.de.entity.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class BurSarFeeFinesJobCommandBuilder implements JobCommandBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobParameters buildJobCommand(Job job) {
    Map<String, JobParameter> params = new HashMap<>();
    try {
      params.put("bursarFeeFines",
        new JobParameter(objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getBursarFeeFines())));
      return new JobParameters(params);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
