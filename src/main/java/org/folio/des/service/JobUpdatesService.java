package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.JobExecutionUpdateDto;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.JobRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobUpdatesService {

  private static final String DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME = "dataExportJobExecutionUpdatesTopic";

  private final JobRepository repository;

  @KafkaListener(topics = { DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME })
  public void receiveJobExecutionUpdate(JobExecutionUpdateDto jobExecutionUpdate) {
    Optional<Job> jobOptional = repository.findById(jobExecutionUpdate.getJobId());
    if (jobOptional.isEmpty()) {
      log.error("Got update for unknown job {}", jobExecutionUpdate.getJobId());
      return;
    }
    Job job = jobOptional.get();

    job.setBatchStatus(jobExecutionUpdate.getStatus());
    job.setStartTime(jobExecutionUpdate.getStartTime());
    job.setCreatedDate(jobExecutionUpdate.getCreateTime());
    job.setEndTime(jobExecutionUpdate.getEndTime());
    job.setUpdatedDate(jobExecutionUpdate.getLastUpdated());

    repository.save(job);
  }

}
