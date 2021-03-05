package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.JobExecutionUpdateDto;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.JobRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobUpdatesService {

  private static final String DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME = "dataExportJobExecutionUpdatesTopic";

  private final JobRepository repository;

  @KafkaListener(topics = { DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME })
  public void receiveJobExecutionUpdate(JobExecutionUpdateDto jobExecutionUpdate) {
    log.info("Received {}.", jobExecutionUpdate);

    Optional<Job> jobOptional = repository.findById(jobExecutionUpdate.getJobId());
    if (jobOptional.isEmpty()) {
      log.error("Update for unknown job {}.", jobExecutionUpdate.getJobId());
      return;
    }
    Job job = jobOptional.get();

    boolean changed = false;
    if (jobExecutionUpdate.getStatus() != null && jobExecutionUpdate.getStatus() != job.getBatchStatus()) {
      job.setBatchStatus(jobExecutionUpdate.getStatus());
      changed = true;
    }
    if (jobExecutionUpdate.getStartTime() != null && !jobExecutionUpdate.getStartTime().equals(job.getStartTime())) {
      job.setStartTime(jobExecutionUpdate.getStartTime());
      changed = true;
    }
    if (jobExecutionUpdate.getEndTime() != null && !jobExecutionUpdate.getEndTime().equals(job.getEndTime())) {
      job.setEndTime(jobExecutionUpdate.getEndTime());
      changed = true;
    }

    if (changed) {
      job.setUpdatedDate(new Date());
      job = repository.save(job);
      log.info("Updated {}.", job);
    }
  }

}
