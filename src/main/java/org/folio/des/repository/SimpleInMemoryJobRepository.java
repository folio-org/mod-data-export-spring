package org.folio.des.repository;

import org.folio.des.domain.entity.Job;
import org.folio.des.domain.entity.JobExecution;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class SimpleInMemoryJobRepository implements IJobRepository {

  private final Map<UUID, Job> jobStorage = new HashMap<>();

  public Job createJob(Job job) {
    job.setId(UUID.randomUUID());
    this.jobStorage.put(job.getId(), job);

    return job;
  }

  public void updateJobExecution(UUID jobId, JobExecution jobExecution) {
    Job job = this.jobStorage.get(jobId);
    job.setJobExecution(jobExecution);
    this.jobStorage.replace(jobId, job);
  }

  public Job getJob(UUID jobId) {
    return this.jobStorage.get(jobId);
  }

}
