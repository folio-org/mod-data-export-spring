package org.folio.model.services;

import org.folio.dto.StartJobCommandDto;
import org.folio.model.repositories.IJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobExecutionService {

    private static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

    private KafkaTemplate<String, StartJobCommandDto> kafkaTemplate;

    @Autowired
    public JobExecutionService(IJobRepository jobRepository, KafkaTemplate<String, StartJobCommandDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void startJob(StartJobCommandDto startJobCommand) {
        this.kafkaTemplate.send(DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME, startJobCommand);
    }
}
