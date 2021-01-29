package org.folio.controllers;

import org.folio.dto.JobDto;
import org.folio.dto.StartJobCommandDto;
import org.folio.dto.StartJobRequestDto;
import org.folio.model.entities.Job;
import org.folio.model.repositories.IJobRepository;
import org.folio.model.services.JobExecutionService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class DataExportController {

    private ModelMapper modelMapper = new ModelMapper();

    private IJobRepository jobRepository;

    private JobExecutionService jobExecutionService;

    @Autowired
    public DataExportController(JobExecutionService jobExecutionService, IJobRepository jobRepository) {
        this.jobExecutionService = jobExecutionService;
        this.jobRepository = jobRepository;
    }

    @PostMapping("/exportJob/start")
    public JobDto startExportJob(@RequestBody StartJobRequestDto startJobRequest) {

        Job createdJob = this.modelMapper.map(startJobRequest, Job.class);
        StartJobCommandDto startJobCommandDto = this.modelMapper.map(startJobRequest, StartJobCommandDto.class);

        createdJob = this.jobRepository.createJob(createdJob);
        startJobCommandDto.setId(createdJob.getId());

        this.jobExecutionService.startJob(startJobCommandDto);

        JobDto jobDto = this.modelMapper.map(createdJob, JobDto.class);
        return jobDto;
    }

    @GetMapping("/exportJob/{jobId}")
    public JobDto getExportJob(@PathVariable("jobId") UUID jobId) {
        Job job = this.jobRepository.getJob(jobId);
        JobDto jobDto = this.modelMapper.map(job, JobDto.class);

        return jobDto;
    }
}
