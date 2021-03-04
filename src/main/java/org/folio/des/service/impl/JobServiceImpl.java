package org.folio.des.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.*;
import org.folio.des.repository.JobRepository;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobServiceImpl implements JobService {

  private final JobExecutionService jobExecutionService;
  private final JobRepository repository;

  @Override
  public Job get(UUID id) {
    Optional<org.folio.des.domain.entity.Job> jobOptional = repository.findById(id);
    if (jobOptional.isEmpty()) {
      throw new NotFoundException(String.format("Job %s not found", id));
    }
    return entityToDto(jobOptional.get());
  }

  @Override
  public JobCollection get(Integer offset, Integer limit, String query) {
    List<Job> jobs = repository.findAll(new OffsetRequest(offset, limit)).map(JobServiceImpl::entityToDto).getContent();
    JobCollection result = new JobCollection();
    result.setJobRecords(jobs);
    result.setTotalRecords(jobs.size());
    return result;
  }

  @Override
  public Job upsert(Job job) {
    org.folio.des.domain.entity.Job jobEntity = dtoToEntity(job);
    jobExecutionService.startJob(prepareStartJobCommand(jobEntity));
    return entityToDto(repository.save(jobEntity));
  }

  @Override
  public void delete(UUID id) {
    repository.deleteById(id);
  }

  private StartJobCommandDto prepareStartJobCommand(org.folio.des.domain.entity.Job job) {
    if (job.getType() == ExportType.BURSAR_FEES_FINES && job.getExportTypeSpecificParameters().getBursarFeeFines() == null) {
      throw new IllegalArgumentException(
          String.format("Job of %s type should contain %s parameters", job.getType(), BursarFeeFines.class.getSimpleName()));
    }

    StartJobCommandDto result = new StartJobCommandDto();
    result.setId(job.getId());
    result.setName(job.getName());
    result.setDescription(job.getDescription());
    result.setExportType(job.getType());

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

  public static Job entityToDto(org.folio.des.domain.entity.Job entity) {
    Job result = new Job();

    result.setId(entity.getId());
    result.setName(entity.getName());
    result.setDescription(entity.getDescription());
    result.setSource(entity.getSource());
    result.setIsSystemSource(entity.getIsSystemSource());
    result.setType(entity.getType());

    if (entity.getExportTypeSpecificParameters() != null) {
      ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();

      if (entity.getExportTypeSpecificParameters().getBursarFeeFines() != null) {
        BursarFeeFines bursarFeeFines = new BursarFeeFines();
        bursarFeeFines.setFtpUrl(entity.getExportTypeSpecificParameters().getBursarFeeFines().getFtpUrl());
        bursarFeeFines.setDaysOutstanding(entity.getExportTypeSpecificParameters().getBursarFeeFines().getDaysOutstanding());
        bursarFeeFines.setPatronGroups(entity.getExportTypeSpecificParameters().getBursarFeeFines().getPatronGroups());
        exportTypeSpecificParameters.setBursarFeeFines(bursarFeeFines);
      }

      exportTypeSpecificParameters.setQuery(entity.getExportTypeSpecificParameters().getQuery());
      result.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    }

    result.setStatus(entity.getStatus());
    result.setFiles(entity.getFiles());
    result.setStartTime(entity.getStartTime());
    result.setEndTime(entity.getEndTime());

    Metadata metadata = new Metadata();
    metadata.setCreatedDate(entity.getCreatedDate());
    metadata.setCreatedByUserId(entity.getCreatedByUserId());
    metadata.setCreatedByUsername(entity.getCreatedByUsername());
    metadata.setUpdatedDate(entity.getUpdatedDate());
    metadata.setUpdatedByUserId(entity.getUpdatedByUserId());
    metadata.setUpdatedByUsername(entity.getUpdatedByUsername());
    result.setMetadata(metadata);

    result.setOutputFormat(entity.getOutputFormat());
    result.setErrorDetails(entity.getErrorDetails());

    return result;
  }

  public static org.folio.des.domain.entity.Job dtoToEntity(Job dto) {
    org.folio.des.domain.entity.Job result = new org.folio.des.domain.entity.Job();

    result.setId(dto.getId());
    result.setName(dto.getName());
    result.setDescription(dto.getDescription());
    result.setSource(dto.getSource());
    result.setIsSystemSource(dto.getIsSystemSource());
    result.setType(dto.getType());

    if (dto.getExportTypeSpecificParameters() != null) {
      ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();

      if (dto.getExportTypeSpecificParameters().getBursarFeeFines() != null) {
        BursarFeeFines bursarFeeFines = new BursarFeeFines();
        bursarFeeFines.setFtpUrl(dto.getExportTypeSpecificParameters().getBursarFeeFines().getFtpUrl());
        bursarFeeFines.setDaysOutstanding(dto.getExportTypeSpecificParameters().getBursarFeeFines().getDaysOutstanding());
        bursarFeeFines.setPatronGroups(dto.getExportTypeSpecificParameters().getBursarFeeFines().getPatronGroups());
        exportTypeSpecificParameters.setBursarFeeFines(bursarFeeFines);
      }

      exportTypeSpecificParameters.setQuery(dto.getExportTypeSpecificParameters().getQuery());
      result.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    }

    result.setStatus(dto.getStatus());
    result.setFiles(dto.getFiles());
    result.setStartTime(dto.getStartTime());
    result.setEndTime(dto.getEndTime());

    if (dto.getMetadata() != null) {
      result.setCreatedDate(dto.getMetadata().getCreatedDate());
      result.setCreatedByUserId(dto.getMetadata().getCreatedByUserId());
      result.setCreatedByUsername(dto.getMetadata().getCreatedByUsername());
      result.setUpdatedDate(dto.getMetadata().getUpdatedDate());
      result.setUpdatedByUserId(dto.getMetadata().getUpdatedByUserId());
      result.setUpdatedByUsername(dto.getMetadata().getUpdatedByUsername());
    }

    result.setOutputFormat(dto.getOutputFormat());
    result.setErrorDetails(dto.getErrorDetails());

    return result;
  }

}
