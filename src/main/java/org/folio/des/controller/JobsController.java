package org.folio.des.controller;

import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.rest.resource.JobsApi;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JobsController implements JobsApi {

  @Override
  public ResponseEntity<Job> getJobById(UUID id) {
    return null;
  }

  @Override
  public ResponseEntity<JobCollection> getJobs(@Valid String name, @Valid List<JobStatus> status, @Valid List<ExportType> type,
      @Valid Boolean isSystemSource, @Valid List<String> source, @Valid List<Date> startTime, @Valid List<Date> endTime) {
    return null;
  }

  @Override
  public ResponseEntity<Job> upsertJob(@Valid Job job) {
    return null;
  }

}
