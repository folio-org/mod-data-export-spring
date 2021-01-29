package org.folio.model.services;

import org.folio.dto.JobExecutionUpdateDto;
import org.folio.model.entities.JobExecution;
import org.folio.model.repositories.IJobRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class JobUpdatesService {

    private static final String DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME = "dataExportJobExecutionUpdatesTopic";

    private ModelMapper modelMapper = new ModelMapper();

    private IJobRepository jobRepository;

    @Autowired
    public JobUpdatesService(IJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @KafkaListener(topics = { DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME })
    public void receiveJobExecutionUpdate(JobExecutionUpdateDto jobExecutionUpdate) {
        JobExecution jobExecution = this.modelMapper.map(jobExecutionUpdate, JobExecution.class);
        this.jobRepository.updateJobExecution(jobExecutionUpdate.getJobId(), jobExecution);
    }
}
