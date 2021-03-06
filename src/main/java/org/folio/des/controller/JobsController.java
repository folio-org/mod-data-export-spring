package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.rest.resource.JobsApi;
import org.folio.des.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.UUID;

@RestController
@RequestMapping("/data-export-spring")
@RequiredArgsConstructor
public class JobsController implements JobsApi {

  private final JobService service;

  @Override
  public ResponseEntity<Job> getJobById(UUID id) {
    return ResponseEntity.ok(service.get(id));
  }

  @Override
  public ResponseEntity<JobCollection> getJobs(@Min(0) @Max(2147483647) @Valid Integer offset,
      @Min(0) @Max(2147483647) @Valid Integer limit, @Valid String query) {
    return ResponseEntity.ok(service.get(offset, limit, query));
  }

  @Override
  public ResponseEntity<Job> upsertJob(@Valid Job job) {
    return new ResponseEntity<>(service.upsert(job), job.getId() == null ? HttpStatus.CREATED : HttpStatus.OK);
  }

}
