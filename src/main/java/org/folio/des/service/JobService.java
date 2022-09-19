package org.folio.des.service;

import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;

import java.io.InputStream;
import java.util.UUID;

public interface JobService {

  /**
   * Gets job by id.
   *
   * @param id the job id
   * @return job by id
   */
  Job get(UUID id);

  /**
   * Gets job collection by search query.
   *
   * @param offset the offset
   * @param limit the limit
   * @param query the query
   * @return job collection
   */
  JobCollection get(Integer offset, Integer limit, String query);

  /**
   * Inserts or updates job, if @withJobCommandSend enabled - send job to kafka
   *
   * @param job the job to upsert
   * @param withJobCommandSend if true - job will be send to kafka or false otherwise
   * @return updated job
   */
  Job upsertAndSendToKafka(Job job, boolean withJobCommandSend);

  /**
   * Deletes old jobs.
   */
  void deleteOldJobs();

  /**
   * Downloading the exported file. A job can have only one exported file.
   * @param jobId the job id
   * @return Input stream of exported file.
   */
  InputStream downloadExportedFile(UUID jobId);

}
