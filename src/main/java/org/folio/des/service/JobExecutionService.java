package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.StartJobCommandDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobExecutionService {

  public static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

  private final KafkaTemplate<String, StartJobCommandDto> kafkaTemplate;

  public void startJob(StartJobCommandDto startJobCommand) {
    log.info("Sending {}.", startJobCommand);
    kafkaTemplate.send(DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME, startJobCommand.getId().toString(), startJobCommand);
    log.info("Sent job {} start.", startJobCommand.getId());
  }

}
