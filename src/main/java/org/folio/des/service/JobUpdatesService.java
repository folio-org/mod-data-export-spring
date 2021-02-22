package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.JobExecutionUpdateDto;
import org.folio.des.domain.entity.JobExecution;
import org.folio.des.repository.IJobRepository;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobUpdatesService {

  private static final String DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME = "dataExportJobExecutionUpdatesTopic";

  private final ModelMapper modelMapper = new ModelMapper();

  private final IJobRepository jobRepository;

  @KafkaListener(topics = { DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME })
  public void receiveJobExecutionUpdate(JobExecutionUpdateDto jobExecutionUpdate) {
    JobExecution jobExecution = this.modelMapper.map(jobExecutionUpdate, JobExecution.class);
    this.jobRepository.updateJobExecution(jobExecutionUpdate.getJobId(), jobExecution);
  }

}
