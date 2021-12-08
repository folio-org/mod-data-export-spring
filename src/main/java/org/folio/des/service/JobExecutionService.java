package org.folio.des.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.JobCommand;
import org.folio.des.domain.entity.Job;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class  JobExecutionService {

  private final KafkaService kafka;
  private final ObjectMapper objectMapper;
  private final ExportConfigValidatorResolver exportConfigValidatorResolver;

  public JobCommand prepareStartJobCommand(Job job) {
    validateIncomingExportConfig(job);

    JobCommand jobCommand = buildBaseJobCommand(job);

    Map<String, JobParameter> params = new HashMap<>();
    if (job.getType() == ExportType.CIRCULATION_LOG) {
      params.put("query", new JobParameter(job.getExportTypeSpecificParameters().getQuery()));
    } else if (job.getType() == ExportType.BURSAR_FEES_FINES) {
      try {
        params.put("bursarFeeFines",
            new JobParameter(objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getBursarFeeFines())));
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e);
      }
    }
    jobCommand.setJobParameters(new JobParameters(params));

    return jobCommand;
  }

  public void sendJobCommand(JobCommand jobCommand) {
    kafka.send(KafkaService.Topic.JOB_COMMAND, jobCommand.getId().toString(), jobCommand);
  }

  public void deleteJobs(List<Job> jobs) {
    List<String> files = jobs.stream()
        .map(Job::getFiles)
        .filter(CollectionUtils::isNotEmpty)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(files)) {
      return;
    }

    var jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.DELETE);
    jobCommand.setId(UUID.randomUUID());
    jobCommand.setJobParameters(new JobParameters(
        Collections.singletonMap(JobParameterNames.OUTPUT_FILES_IN_STORAGE, new JobParameter(StringUtils.join(files, ';')))));
    sendJobCommand(jobCommand);
  }

  protected void validateIncomingExportConfig(Job job) {
    exportConfigValidatorResolver.resolve(job.getType(), ExportTypeSpecificParameters.class).ifPresent(validator -> {
      Errors errors = new BeanPropertyBindingResult(job.getExportTypeSpecificParameters(), "specificParameters");
      validator.validate(job.getExportTypeSpecificParameters(), errors);
    });
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
