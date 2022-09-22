package org.folio.des.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.Job;
import org.folio.de.entity.JobCommand;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.spring.exception.NotFoundException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class  JobExecutionService {

  private final KafkaService kafka;
  private final ExportConfigValidatorResolver exportConfigValidatorResolver;
  private final JobCommandBuilderResolver jobCommandBuilderResolver;
  private final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  private final ObjectMapper objectMapper;
  private final ConfigurationClient manager;

  public static final String EXPORT_CONFIGURATION_NOT_FOUND = "Export configuration not found or parse error : %s";

  public JobCommand prepareStartJobCommand(Job job) {
    validateIncomingExportConfig(job);

    JobCommand jobCommand = buildBaseJobCommand(job);

    jobCommandBuilderResolver.resolve(job.getType()).ifPresentOrElse(builder -> {
        JobParameters jobParameters = builder.buildJobCommand(job);
        jobCommand.setJobParameters(jobParameters);
      },
      () -> jobCommand.setJobParameters(new JobParameters(new HashMap<>())));
    return jobCommand;
  }

  public JobCommand prepareResendJobCommand(Job job) {
    validateIncomingExportConfig(job);
    JobCommand jobCommand = buildResendJobCommand(job);

    Map<String, JobParameter> params = new HashMap<>();
    String exportConfigId = job.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getExportConfigId().toString();

    ModelConfiguration modelConfiguration = manager.getConfigById(exportConfigId);

    if (modelConfiguration == null) {
      throw new NotFoundException(String.format(EXPORT_CONFIGURATION_NOT_FOUND, exportConfigId));
    }
    ExportConfig config = defaultModelConfigToExportConfigConverter.convert(modelConfiguration);

    jobCommandBuilderResolver.resolve(job.getType()).ifPresentOrElse(builder -> {
        try {
          params.put("edifactOrdersExport",
          new JobParameter(objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig())));
          params.put("fileName", new JobParameter(objectMapper.writeValueAsString(job.getFileNames())));
          params.put("ediFtp", new JobParameter(objectMapper.writeValueAsString(config.getExportTypeSpecificParameters()
            .getVendorEdiOrdersExportConfig()
            .getEdiFtp())));

        }  catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
        jobCommand.setJobParameters(new JobParameters(params));
      },
      () -> jobCommand.setJobParameters(new JobParameters(new HashMap<>())));

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

  private JobCommand buildResendJobCommand(Job job) {
    var result = new JobCommand();
    result.setType(JobCommand.Type.RESEND);
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
