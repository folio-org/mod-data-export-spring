package org.folio.des.controller;

import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.rest.resource.JobsApi;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.UUID;

public class JobsController implements JobsApi {

  @Override
  public ResponseEntity<Job> getJobById(UUID id) {
    return null;
  }

  @Override
  public ResponseEntity<JobCollection> getJobs(@Min(0) @Max(2147483647) @Valid Integer offset,
      @Min(0) @Max(2147483647) @Valid Integer limit, @Valid String query) {
    return null;
  }

  @Override
  public ResponseEntity<Job> upsertJob(@Valid Job job) {
    return null;
  }

}
