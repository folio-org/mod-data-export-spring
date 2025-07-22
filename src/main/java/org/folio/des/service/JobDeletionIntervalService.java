package org.folio.des.service;

import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.delete_interval.JobDeletionInterval;
import org.folio.des.domain.dto.delete_interval.JobDeletionIntervalCollection;

public interface JobDeletionIntervalService {
  /**
   * Get all job deletion intervals.
   *
   * @return collection of job deletion intervals
   */
  JobDeletionIntervalCollection getAll();

  /**
   * Get interval by export type.
   *
   * @param exportType the export type to retrieve
   * @return the found job deletion interval
   */
  JobDeletionInterval get(ExportType exportType);

  /**
   * Create a new job deletion interval.
   *
   * @param interval the interval to create
   * @return the created interval
   */
  JobDeletionInterval create(JobDeletionInterval interval);

  /**
   * Update an existing job deletion interval.
   *
   * @param interval the interval to update
   * @return the updated interval
   */
  JobDeletionInterval update(JobDeletionInterval interval);

  /**
   * Delete a job deletion interval.
   *
   * @param exportType the export type to delete
   */
  void delete(ExportType exportType);
}
