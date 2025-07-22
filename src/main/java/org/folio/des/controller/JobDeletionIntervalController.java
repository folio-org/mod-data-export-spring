package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.delete_interval.JobDeletionInterval;
import org.folio.des.domain.dto.delete_interval.JobDeletionIntervalCollection;
import org.folio.des.rest.resource.JobDeletionIntervalsApi;
import org.folio.des.service.JobDeletionIntervalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-export-spring")
@RequiredArgsConstructor
public class JobDeletionIntervalController implements JobDeletionIntervalsApi {
  private final JobDeletionIntervalService service;

  @Override
  public ResponseEntity<JobDeletionIntervalCollection> getAllJobDeletionIntervals() {
    var intervals = service.getAll();
    return ResponseEntity.ok(intervals);
  }

  @Override
  public ResponseEntity<JobDeletionInterval> getJobDeletionIntervalByExportType(ExportType exportType) {
    return ResponseEntity.ok(service.get(exportType));
  }

  @Override
  public ResponseEntity<JobDeletionInterval> createJobDeletionInterval(JobDeletionInterval interval) {
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(service.create(interval));
  }

  @Override
  public ResponseEntity<JobDeletionInterval> updateJobDeletionInterval(ExportType exportType, JobDeletionInterval interval) {
    interval.setExportType(exportType);
    return ResponseEntity.ok(service.update(interval));
  }

  @Override
  public ResponseEntity<Void> deleteJobDeletionInterval(ExportType exportType) {
    service.delete(exportType);
    return ResponseEntity.noContent().build();
  }
}
