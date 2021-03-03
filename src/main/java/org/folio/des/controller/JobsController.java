package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.*;
import org.folio.des.repository.IJobRepository;
import org.folio.des.rest.resource.JobsApi;
import org.folio.des.service.ExportConfigService;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class JobsController implements JobsApi {

  private final ModelMapper modelMapper = new ModelMapper();
  private final IJobRepository jobRepository;
  private final JobService jobService;
  private final ExportConfigService exportConfigService;
  private final JobExecutionService jobExecutionService;

  @Override
  public ResponseEntity<Job> getJobById(UUID id) {
    return new ResponseEntity<>(jobService.get(id), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<JobCollection> getJobs(@Min(0) @Max(2147483647) @Valid Integer offset,
      @Min(0) @Max(2147483647) @Valid Integer limit, @Valid String query) {
    return new ResponseEntity<>(jobService.get(offset, limit, query), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Job> upsertJob(@Valid Job job) {
    org.folio.des.domain.entity.Job createdJob = modelMapper.map(job, org.folio.des.domain.entity.Job.class);
    StartJobCommandDto startJobCommandDto = modelMapper.map(job, StartJobCommandDto.class);

    createdJob = jobRepository.createJob(createdJob);
    var config = exportConfigService.getConfig();

    //todo bursar hardcode
    if (config.isPresent()) {
      prepareJob(createdJob, startJobCommandDto, config.get());

      jobExecutionService.startJob(startJobCommandDto);

      var savedJob = modelMapper.map(createdJob, Job.class);
      return ResponseEntity.ok(savedJob);
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  private void prepareJob(org.folio.des.domain.entity.Job createdJob, StartJobCommandDto startJobCommandDto,
      ExportConfig exportConfig) {
    BursarFeeFines bursarFeeFines = exportConfig.getExportTypeSpecificParameters().getBursarFeeFines();
    Long daysOutstanding = (long) bursarFeeFines.getDaysOutstanding();
    String patronGroups = String.join(",", bursarFeeFines.getPatronGroups());

    Map<String, JobParameterDto> params = new HashMap<>();
    params.put("daysOutstanding", new JobParameterDto(daysOutstanding));
    params.put("patronGroups", new JobParameterDto(patronGroups));

    startJobCommandDto.setId(createdJob.getId());
    startJobCommandDto.setName("name");
    startJobCommandDto.setDescription("desc");
    startJobCommandDto.setJobInputParameters(params);
  }

}
