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
   * Inserts or updates job, validates job's config presence, if @withJobCommandSend enabled - send job to kafka
   *
   * @param job the job to upsert
   * @param withJobCommandSend if true - job will be send to kafka or false otherwise
   * @return updated job
   */
  Job upsertAndSendToKafka(Job job, boolean withJobCommandSend);

  /**
   * Inserts or updates job, validates job's config presence, if @withJobCommandSend enabled - send job to kafka
   *
   * @param job the job to upsert
   * @param withJobCommandSend if true - job will be send to kafka or false otherwise
   * @param validateConfigPresence if true - checks that job's config is present
   * @return updated job
   */
  Job upsertAndSendToKafka(Job job, boolean withJobCommandSend, boolean validateConfigPresence);

  /**
   * Deletes old jobs.
   * This method skips deleting EDIFACT_ORDERS_EXPORT and CLAIMS jobs.
   * For bulk edit jobs, it uses custom expiration period defined in mod-configuration(by default 14 days).
   * For all other jobs, it uses default expiration period 7 days(configurable).
   */
  void deleteOldJobs();

  /**
   * Resending the exported file. A job can have only one exported file.
   * @param jobId the job id
   */
  void resendExportedFile(UUID jobId);

  /**
   * Downloading the exported file. A job can have only one exported file.
   * @param jobId the job id
   * @param key the key of the file in the storage
   * @return Input stream of exported file.
   */
  InputStream downloadExportedFile(UUID jobId, String key);

}
