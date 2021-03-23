package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobCommand;
import org.folio.des.domain.entity.Job;
import org.folio.des.service.impl.ExportConfigServiceImpl;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobExecutionService {

  public static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

  private final KafkaTemplate<String, JobCommand> kafkaTemplate;

  public static JobCommand prepareStartJobCommand(Job job) {
    ExportConfigServiceImpl.checkConfig(job.getType(), job.getExportTypeSpecificParameters());

    JobCommand result = new JobCommand();
    result.setType(JobCommand.Type.START);
    result.setId(job.getId());
    result.setName(job.getName());
    result.setDescription(job.getDescription());
    result.setExportType(job.getType());

    Map<String, JobParameter> params = new HashMap<>();
    if (job.getType() == ExportType.CIRCULATION_LOG) {
      params.put("query", new JobParameter(job.getExportTypeSpecificParameters().getQuery()));
    } else if (job.getType() == ExportType.BURSAR_FEES_FINES) {
      BursarFeeFines bursarFeeFines = job.getExportTypeSpecificParameters().getBursarFeeFines();
      params.put("daysOutstanding", new JobParameter((long) bursarFeeFines.getDaysOutstanding()));
      params.put("patronGroups", new JobParameter(String.join(",", bursarFeeFines.getPatronGroups())));
    }
    result.setJobParameters(new JobParameters(params));

    return result;
  }

  public void sendJobCommand(JobCommand jobCommand) {
    log.info("Sending {}.", jobCommand);
    kafkaTemplate.send(DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME, jobCommand.getId().toString(), jobCommand);
    log.info("Sent job {}.", jobCommand.getId());
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

    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.DELETE);
    jobCommand.setId(UUID.randomUUID());
    jobCommand.setJobParameters(new JobParameters(
        Collections.singletonMap(JobParameterNames.OUTPUT_FILES_IN_STORAGE, new JobParameter(StringUtils.join(files, ';')))));
    sendJobCommand(jobCommand);
  }

}
