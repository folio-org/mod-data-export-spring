package org.folio.des.builder.job;

import org.folio.de.entity.JobCommand;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Log4j2
@RequiredArgsConstructor
public class EdifactOrdersJobCommandSchedulerBuilder implements JobCommandSchedulerBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobCommand buildJobCommand(org.folio.des.domain.dto.Job job) {
    JobCommand jobCommand = buildBaseJobCommand(job);
    var paramsBuilder = new JobParametersBuilder();
    try {
      paramsBuilder.addString("edifactOrdersExport",
        objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig()));
      jobCommand.setJobParameters(paramsBuilder.toJobParameters());
      return jobCommand;
    } catch (JacksonException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private JobCommand buildBaseJobCommand(org.folio.des.domain.dto.Job job) {
    var result = new JobCommand();
    result.setId(job.getId());
    result.setExportType(job.getType());
    result.setType(JobCommand.Type.START);
    result.setIdentifierType(job.getIdentifierType());
    result.setEntityType(job.getEntityType());
    result.setProgress(job.getProgress());
    result.setName(job.getName());
    result.setDescription(job.getDescription());
    return result;
  }
}
