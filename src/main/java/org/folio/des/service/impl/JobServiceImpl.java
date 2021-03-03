package org.folio.des.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.*;
import org.folio.des.repository.JobRepository;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobServiceImpl implements JobService {

  private final ModelMapper modelMapper = new ModelMapper();
  private final JobExecutionService jobExecutionService;
  private final JobRepository repository;

  @Override
  public Job get(UUID id) {
    Optional<org.folio.des.domain.entity.Job> jobOptional = repository.findById(id);
    if (jobOptional.isEmpty()) {
      throw new NotFoundException(String.format("Job %s not found", id));
    }
    return modelMapper.map(jobOptional.get(), Job.class);
  }

  @Override
  public JobCollection get(Integer offset, Integer limit, String query) {
    List<Job> jobs = repository.findAll(new OffsetRequest(offset, limit)).map(j -> modelMapper.map(j, Job.class)).getContent();
    JobCollection result = new JobCollection();
    result.setJobRecords(jobs);
    result.setTotalRecords(jobs.size());
    return result;
  }

  @Override
  public Job upsert(Job job) {
    org.folio.des.domain.entity.Job jobEntity = modelMapper.map(job, org.folio.des.domain.entity.Job.class);
    jobExecutionService.startJob(prepareStartJobCommand(job));
    return modelMapper.map(repository.save(jobEntity), Job.class);
  }

  @Override
  public void delete(UUID id) {
    repository.deleteById(id);
  }

  private StartJobCommandDto prepareStartJobCommand(Job job) {
    if (job.getType() == ExportType.BURSAR_FEES_FINES && job.getExportTypeSpecificParameters().getBursarFeeFines() == null) {
      throw new IllegalArgumentException(
          String.format("Job of %s type should contain %s parameters", job.getType(), BursarFeeFines.class.getSimpleName()));
    }

    StartJobCommandDto result = modelMapper.map(job, StartJobCommandDto.class);

    Map<String, JobParameterDto> params = new HashMap<>();
    if (job.getType() == ExportType.CIRCULATION_LOG) {
      params.put("query", new JobParameterDto(job.getExportTypeSpecificParameters().getQuery()));
    } else if (job.getType() == ExportType.BURSAR_FEES_FINES) {
      BursarFeeFines bursarFeeFines = job.getExportTypeSpecificParameters().getBursarFeeFines();
      params.put("daysOutstanding", new JobParameterDto((long) bursarFeeFines.getDaysOutstanding()));
      params.put("patronGroups", new JobParameterDto(String.join(",", bursarFeeFines.getPatronGroups())));
    }
    result.setJobInputParameters(params);

    return result;
  }

}
