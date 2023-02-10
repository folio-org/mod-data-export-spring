package org.folio.des.builder.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.JobCommand;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class EdifactOrdersJobCommandSchedulerBuilder implements JobCommandSchedulerBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobCommand buildJobCommand(org.folio.des.domain.dto.Job job) {
    var jobCommand = buildBaseJobCommand(job);
    try {
      var parametersBuilder = new JobParametersBuilder();
      var vetVendorEdiOrdersExportConfig = Optional.ofNullable(
        objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig()));

      vetVendorEdiOrdersExportConfig.ifPresent(config -> parametersBuilder.addString("edifactOrdersExport",config));
      jobCommand.setJobParameters(parametersBuilder.toJobParameters());

      return jobCommand;
    } catch (JsonProcessingException e) {
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
