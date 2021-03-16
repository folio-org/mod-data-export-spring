package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.StartJobCommand;
import org.folio.des.domain.entity.Job;
import org.folio.des.service.impl.ExportConfigServiceImpl;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobExecutionService {

  public static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

  private final KafkaTemplate<String, StartJobCommand> kafkaTemplate;

  public static StartJobCommand prepareStartJobCommand(Job job) {
    ExportConfigServiceImpl.checkConfig(job.getType(), job.getExportTypeSpecificParameters());

    StartJobCommand result = new StartJobCommand();
    result.setId(job.getId());
    result.setName(job.getName());
    result.setDescription(job.getDescription());
    result.setType(job.getType());

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

  public void startJob(StartJobCommand startJobCommand) {
    log.info("Sending {}.", startJobCommand);
    kafkaTemplate.send(DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME, startJobCommand.getId().toString(), startJobCommand);
    log.info("Sent job {}.", startJobCommand.getId());
  }

}
