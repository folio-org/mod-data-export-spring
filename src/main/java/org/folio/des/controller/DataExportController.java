package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.JobDto;
import org.folio.des.domain.dto.StartJobCommandDto;
import org.folio.des.domain.dto.StartJobRequestDto;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.IJobRepository;
import org.folio.des.service.JobExecutionService;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DataExportController {

  private final ModelMapper modelMapper = new ModelMapper();

  private final IJobRepository jobRepository;
  private final JobExecutionService jobExecutionService;

  @PostMapping("/exportJob/start")
  public JobDto startExportJob(@RequestBody StartJobRequestDto startJobRequest) {

    Job createdJob = modelMapper.map(startJobRequest, Job.class);
    StartJobCommandDto startJobCommandDto = modelMapper.map(startJobRequest, StartJobCommandDto.class);

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

}
