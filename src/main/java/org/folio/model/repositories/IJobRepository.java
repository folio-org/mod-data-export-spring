package org.folio.model.repositories;

import org.folio.model.entities.Job;
import org.folio.model.entities.JobExecution;

import java.util.UUID;

public interface IJobRepository {

    Job createJob(Job job);

    void updateJobExecution(UUID jobId, JobExecution jobExecution);

    Job getJob(UUID jobId);
}
