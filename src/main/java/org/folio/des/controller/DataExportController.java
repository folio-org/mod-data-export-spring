package org.folio.des.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.BursarExportConfig;
import org.folio.des.domain.dto.JobDto;
import org.folio.des.domain.dto.JobParameterDto;
import org.folio.des.domain.dto.StartJobCommandDto;
import org.folio.des.domain.dto.StartJobRequestDto;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.IJobRepository;
import org.folio.des.rest.resource.JobsApi;
import org.folio.des.service.ConfigBursarExportService;
import org.folio.des.service.JobExecutionService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("data-export-spring")
public class DataExportController implements JobsApi {

  private final ModelMapper modelMapper = new ModelMapper();
  private final ConfigBursarExportService configBursarExportService;
  private final IJobRepository jobRepository;
  private final JobExecutionService jobExecutionService;

  @PostMapping("/exportJob/start")
  public JobDto startExportJob(@RequestBody StartJobRequestDto startJobRequest) {

    Job createdJob = modelMapper.map(startJobRequest, Job.class);
    StartJobCommandDto startJobCommandDto =
        modelMapper.map(startJobRequest, StartJobCommandDto.class);

    createdJob = jobRepository.createJob(createdJob);
    startJobCommandDto.setId(createdJob.getId());

    jobExecutionService.startJob(startJobCommandDto);

    return modelMapper.map(createdJob, JobDto.class);
  }

  @GetMapping("/exportJob/{jobId}")
  public JobDto getExportJob(@PathVariable("jobId") UUID jobId) {
    Job job = jobRepository.getJob(jobId);

    return modelMapper.map(job, JobDto.class);
  }

  @Override
  public ResponseEntity<org.folio.des.domain.dto.Job> upsertJob(
      org.folio.des.domain.dto.@Valid Job job) {

    Job createdJob = modelMapper.map(job, Job.class);
    StartJobCommandDto startJobCommandDto = modelMapper.map(job, StartJobCommandDto.class);

    createdJob = jobRepository.createJob(createdJob);
    var config = configBursarExportService.getConfig();

    //todo bursar hardcode
    if (config.isPresent()) {
      prepareJob(createdJob, startJobCommandDto, config.get());

      jobExecutionService.startJob(startJobCommandDto);

      var savedJob = modelMapper.map(createdJob, org.folio.des.domain.dto.Job.class);
      return ResponseEntity.ok(savedJob);
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  private void prepareJob(
      Job createdJob,
      StartJobCommandDto startJobCommandDto,
      BursarExportConfig bursarExportConfig) {
    Long daysOutstanding = (long) bursarExportConfig.getDaysOutstanding();
    String patronGroups = String.join(",", bursarExportConfig.getPatronGroups());

    Map<String, JobParameterDto> params = new HashMap<>();
    params.put("daysOutstanding", new JobParameterDto(daysOutstanding));
    params.put("patronGroups", new JobParameterDto(patronGroups));

    startJobCommandDto.setId(createdJob.getId());
    startJobCommandDto.setName("name");
    startJobCommandDto.setDescription("desc");
    startJobCommandDto.setJobInputParameters(params);
  }
}
