package org.folio.des.repository;

import org.folio.des.domain.entity.Job;
import org.folio.des.domain.entity.JobExecution;

import java.util.UUID;

public interface IJobRepository {

  Job createJob(Job job);

  void updateJobExecution(UUID jobId, JobExecution jobExecution);

  Job getJob(UUID jobId);

}
