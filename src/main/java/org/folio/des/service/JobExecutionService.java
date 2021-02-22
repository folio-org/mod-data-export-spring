package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.StartJobCommandDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobExecutionService {

  private static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

  private final KafkaTemplate<String, StartJobCommandDto> kafkaTemplate;

  public void startJob(StartJobCommandDto startJobCommand) {
    this.kafkaTemplate.send(DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME, startJobCommand);
  }

}
