package org.folio.des.controller;

import static java.util.Objects.isNull;
import static org.folio.des.domain.dto.ExportType.AUTH_HEADINGS_UPDATES;
import static org.hibernate.internal.util.StringHelper.isBlank;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.builder.job.JobCommandSchedulerBuilder;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.rest.resource.JobsApi;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-export-spring")
@RequiredArgsConstructor
@Log4j2
public class JobsController implements JobsApi {

  private final JobService service;
  private final JobCommandSchedulerBuilder jobCommandSchedulerBuilder;
  private final JobExecutionService jobExecutionService;

  @Override
  public ResponseEntity<Job> getJobById(UUID id) {
    log.info("getJobById:: by id={}.", id);
    return ResponseEntity.ok(service.get(id));
  }

  @Override
  public ResponseEntity<JobCollection> getJobs(Integer offset, Integer limit, String query) {
    log.info("getJobs:: by query={} with offset={} and limit={}.", query, offset, limit);
    return ResponseEntity.ok(service.get(offset, limit, query));
  }

  @Override
  public ResponseEntity<Job> upsertJob(@RequestHeader("X-Okapi-Tenant") String tenantId, Job job) {
    job.setTenant(tenantId);
    log.info("upsertJob:: with job={}.", job);
    if (isMissingRequiredParameters(job)) {
      log.warn("upsertJob: Missing Required Parameters.");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(service.upsertAndSendToKafka(job, true), job.getId() == null ? HttpStatus.CREATED : HttpStatus.OK);
  }

  @Override
  public ResponseEntity resendExportedFile(UUID jobId) {
    log.info("resendExportedFile:: with jobId={}.", jobId);
    service.resendExportedFile(jobId);
    return ResponseEntity.ok(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Resource> downloadExportedFileByJobId(UUID id, String key) {
    log.info("downloadExportedFileByJobId:: with id={}, key={}.", id, key);
    return ResponseEntity.ok(new InputStreamResource(service.downloadExportedFile(id, key)));
  }

  @Override
  public ResponseEntity<Void> sendJob(Job job) {
    log.info("sendJob:: with job={}.", job);
    jobExecutionService.sendJobCommand(jobCommandSchedulerBuilder.buildJobCommand(job));
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private boolean isMissingRequiredParameters(Job job) {
    var exportTypeParameters = job.getExportTypeSpecificParameters();
    return invalidAuthorityControlJob(job, exportTypeParameters);
  }

  private boolean invalidAuthorityControlJob(Job job, ExportTypeSpecificParameters exportTypeParameters) {
    var acConfig = exportTypeParameters.getAuthorityControlExportConfig();

    return AUTH_HEADINGS_UPDATES == job.getType() && acConfig.getFromDate().isAfter(acConfig.getToDate());
  }
}
