package org.folio.des.builder.job;

import java.util.HashMap;
import java.util.Map;

import org.folio.de.entity.JobCommand;
import org.folio.des.domain.dto.Job;
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
public class EdifactOrdersJobCommandSchedulerBuilder implements JobCommandSchedulerBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobCommand buildJobCommand(Job job) {
    JobCommand jobCommand = buildBaseJobCommand(job);
    Map<String, JobParameter> params = new HashMap<>();
    try {
      params.put("edifactOrdersExport",
        new JobParameter(objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig())));
      JobParameters jobParameters = new JobParameters(params);
      jobCommand.setJobParameters(jobParameters);
      return jobCommand;
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private JobCommand buildBaseJobCommand(Job job) {
    var result = new JobCommand();
    result.setType(JobCommand.Type.START);
    result.setId(job.getId());
    result.setName(job.getName());
    result.setDescription(job.getDescription());
    result.setExportType(job.getType());
    result.setIdentifierType(job.getIdentifierType());
    result.setEntityType(job.getEntityType());
    result.setProgress(job.getProgress());
    return result;
  }
}
