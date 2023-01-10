package org.folio.des.builder.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthorityControlJobCommandBuilder implements JobCommandBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobParameters buildJobCommand(Job job) {
    Map<String, JobParameter> params = new HashMap<>();
    try {
      params.put("authorityControlExportConfig",
        new JobParameter(objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getAuthorityControlExportConfig())));
      return new JobParameters(params);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
