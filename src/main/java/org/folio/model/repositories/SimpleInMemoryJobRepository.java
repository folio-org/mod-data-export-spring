package org.folio.model.repositories;

import org.folio.model.entities.Job;
import org.folio.model.entities.JobExecution;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// TODO replace it with PostgreSQL repository

@Repository
public class SimpleInMemoryJobRepository implements IJobRepository {

    private Map<UUID, Job> jobStorage = new HashMap<>();

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
