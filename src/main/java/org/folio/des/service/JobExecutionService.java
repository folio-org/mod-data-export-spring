package org.folio.des.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class  JobExecutionService {

  private final KafkaService kafka;
  private final ExportConfigValidatorResolver exportConfigValidatorResolver;
  private final JobCommandBuilderResolver jobCommandBuilderResolver;
  private final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  private final ConfigurationClient manager;
  private final ObjectMapper objectMapper;
  public static final String EDIFACT_ORDERS_EXPORT_KEY = "EDIFACT_ORDERS_EXPORT";
  public static final String FILE_NAME_KEY = "FILE_NAME";

  public JobCommand prepareStartJobCommand(Job job) {
    validateIncomingExportConfig(job);

    JobCommand jobCommand = buildBaseJobCommand(job, JobCommand.Type.START);

    jobCommandBuilderResolver.resolve(job.getType()).ifPresentOrElse(builder -> {
        JobParameters jobParameters = builder.buildJobCommand(job);
        jobCommand.setJobParameters(jobParameters);
      },
      () -> jobCommand.setJobParameters(new JobParameters(new HashMap<>())));
    return jobCommand;
  }

  public JobCommand prepareResendJobCommand(Job job) {
    validateIncomingExportConfig(job);
    JobCommand jobCommand = buildBaseJobCommand(job, JobCommand.Type.RESEND);

    Map<String, JobParameter> params = new HashMap<>();
    String exportConfigId = job.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getExportConfigId().toString();

    ModelConfiguration modelConfiguration = manager.getConfigById(exportConfigId);
    ExportConfig config = defaultModelConfigToExportConfigConverter.convert(modelConfiguration);
    Optional.ofNullable(config.getExportTypeSpecificParameters()).
      map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
        .ifPresent(vendorEdiOrdersExportConfig -> addToParamsEdiExportConfig(params, vendorEdiOrdersExportConfig));
    Optional.ofNullable(job.getFileNames())
        .ifPresent(fileNames->
          params.put(FILE_NAME_KEY, new JobParameter(String.valueOf(fileNames.get(0)))));

    jobCommand.setJobParameters(new JobParameters(params));

   return jobCommand;
  }

  @SneakyThrows
  private void addToParamsEdiExportConfig(Map<String, JobParameter> params, VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig) {
    params.put(EDIFACT_ORDERS_EXPORT_KEY, new JobParameter(objectMapper.writeValueAsString(vendorEdiOrdersExportConfig)));
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

  private JobCommand buildBaseJobCommand(Job job, JobCommand.Type type) {
    var result = new JobCommand();
    result.setType(type);
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
