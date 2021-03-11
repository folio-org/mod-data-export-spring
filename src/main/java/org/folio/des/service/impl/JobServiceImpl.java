package org.folio.des.service.impl;

import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.dto.Metadata;
import org.folio.des.domain.dto.StartJobCommand;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.JobRepository;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

  private static final Map<ExportType, String> OUTPUT_FORMATS = new EnumMap<>(ExportType.class);

  static {
    OUTPUT_FORMATS.put(ExportType.BURSAR_FEES_FINES, "Cornell Fees & Fines Bursar Report");
    OUTPUT_FORMATS.put(ExportType.CIRCULATION_LOG, "Comma-Separated Values (CSV)");
  }

  private final JobExecutionService jobExecutionService;
  private final JobRepository repository;
  private final FolioExecutionContext context;
  private final FolioExecutionContextHelper contextHelper;
  private final CQLService cqlService;

  @Override
  public org.folio.des.domain.dto.Job get(UUID id) {
    Optional<Job> jobDtoOptional = repository.findById(id);
    if (jobDtoOptional.isEmpty()) {
      throw new NotFoundException(String.format("Job %s not found", id));
    }
    return entityToDto(jobDtoOptional.get());
  }

  @Override
  public JobCollection get(Integer offset, Integer limit, String query) {
    JobCollection result = new JobCollection();
    if (StringUtils.isBlank(query)) {
      Page<Job> page = repository.findAll(new OffsetRequest(offset, limit));
      result.setJobRecords(page.map(JobServiceImpl::entityToDto).getContent());
      result.setTotalRecords((int) page.getTotalElements());
    } else {
      List<Job> jobs = cqlService.getByCQL(Job.class, query, offset, limit);
      List<org.folio.des.domain.dto.Job> jobsResult =
          jobs.stream().map(JobServiceImpl::entityToDto).collect(Collectors.toList());
      result.setJobRecords(jobsResult);

      Integer count = cqlService.countByCQL(Job.class, query);
      result.setTotalRecords(count);
    }
    return result;
  }

  @Override
  public org.folio.des.domain.dto.Job upsert(org.folio.des.domain.dto.Job jobDto) {
    log.info("Upserting DTO {}.", jobDto);
    Job result = dtoToEntity(jobDto);

    if (StringUtils.isBlank(result.getName())) {
      result.setName("Job #TBD");
    }
    String userName = contextHelper.getUserName(context);
    if (StringUtils.isBlank(result.getSource())) {
      result.setSource(userName);
    }
    if (result.getIsSystemSource() == null) {
      result.setIsSystemSource(StringUtils.isBlank(userName));
    }
    if (result.getStatus() == null) {
      result.setStatus(JobStatus.SCHEDULED);
    }
    Date now = new Date();
    if (result.getCreatedDate() == null) {
      result.setCreatedDate(now);
    }
    UUID userId = contextHelper.getUserId(context);
    if (result.getCreatedByUserId() == null) {
      result.setCreatedByUserId(userId);
    }
    if (StringUtils.isBlank(result.getCreatedByUsername())) {
      result.setCreatedByUsername(userName);
    }
    result.setUpdatedDate(now);
    result.setUpdatedByUserId(userId);
    result.setUpdatedByUsername(userName);
    if (StringUtils.isBlank(result.getOutputFormat())) {
      result.setOutputFormat(OUTPUT_FORMATS.get(result.getType()));
    }
    if (result.getBatchStatus() == null) {
      result.setBatchStatus(BatchStatus.UNKNOWN);
    }
    if (result.getExitStatus() == null) {
      result.setExitStatus(ExitStatus.UNKNOWN);
    }

    StartJobCommand startJobCommand = prepareStartJobCommand(result);

    log.info("Upserting {}.", result);
    result = repository.save(result);
    log.info("Upserted {}.", result);

    startJobCommand.setId(result.getId());
    jobExecutionService.startJob(startJobCommand);

    return entityToDto(result);
  }

  @Override
  public void delete(UUID id) {
    repository.deleteById(id);
    log.info("Deleted job {}.", id);
  }

  private StartJobCommand prepareStartJobCommand(Job job) {
    ExportConfigServiceImpl.checkConfig(job.getType(), job.getExportTypeSpecificParameters());

    StartJobCommand result = new StartJobCommand();
    result.setId(job.getId());
    result.setName(job.getName());
    result.setDescription(job.getDescription());
    result.setType(job.getType());

    Map<String, JobParameter> params = new HashMap<>();
    if (job.getType() == ExportType.CIRCULATION_LOG) {
      params.put("query", new JobParameter(job.getExportTypeSpecificParameters().getQuery()));
    } else if (job.getType() == ExportType.BURSAR_FEES_FINES) {
      BursarFeeFines bursarFeeFines = job.getExportTypeSpecificParameters().getBursarFeeFines();
      params.put("daysOutstanding", new JobParameter((long) bursarFeeFines.getDaysOutstanding()));
      params.put("patronGroups", new JobParameter(String.join(",", bursarFeeFines.getPatronGroups())));
    }
    result.setJobParameters(new JobParameters(params));

    return result;
  }

  public static org.folio.des.domain.dto.Job entityToDto(Job entity) {
    org.folio.des.domain.dto.Job result = new org.folio.des.domain.dto.Job();

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

  public static Job dtoToEntity(org.folio.des.domain.dto.Job dto) {
    Job result = new Job();

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
