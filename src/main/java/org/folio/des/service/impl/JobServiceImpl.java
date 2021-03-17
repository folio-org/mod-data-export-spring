package org.folio.des.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableScheduling
@Log4j2
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

  private static final Map<ExportType, String> OUTPUT_FORMATS = new EnumMap<>(ExportType.class);

  static {
    OUTPUT_FORMATS.put(ExportType.BURSAR_FEES_FINES, "Fees & Fines Bursar Report");
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
      result.setJobRecords(cqlService.getByCQL(Job.class, query, offset, limit)
          .stream()
          .map(JobServiceImpl::entityToDto)
          .collect(Collectors.toList()));
      result.setTotalRecords(cqlService.countByCQL(Job.class, query));
    }
    return result;
  }

  @Override
  public org.folio.des.domain.dto.Job upsert(org.folio.des.domain.dto.Job jobDto) {
    log.info("Upserting DTO {}.", jobDto);
    Job result = dtoToEntity(jobDto);

    if (StringUtils.isBlank(result.getName())) {
      result.setName(String.format("%06d", repository.getNextJobNumber()));
    }
    String userName = FolioExecutionContextHelper.getUserName(context);
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
    UUID userId = FolioExecutionContextHelper.getUserId(context);
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

    JobCommand jobCommand = JobExecutionService.prepareStartJobCommand(result);

    log.info("Upserting {}.", result);
    result = repository.save(result);
    log.info("Upserted {}.", result);

    jobCommand.setId(result.getId());
    jobExecutionService.sendJobCommand(jobCommand);

    return entityToDto(result);
  }

  @Override
  public void delete(UUID id) {
    repository.deleteById(id);
    log.info("Deleted job {}.", id);
  }

  @Scheduled(fixedRateString = "P1D")
  @Override
  public void deleteOldJobs() {
    if (contextHelper.isModuleRegistered()) {
      contextHelper.initScope();

      Date toDelete = Date.from(LocalDate.now().minusDays(7).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
      log.info("Deleting old jobs with 'updatedDate' less than {}.", toDelete);

      List<Job> jobs = repository.findByUpdatedDateBefore(toDelete);
      if (CollectionUtils.isEmpty(jobs)) {
        log.info("Deleted no old jobs.");
        return;
      }

      List<UUID> ids = jobs.stream().map(Job::getId).collect(Collectors.toList());
      repository.deleteByIdIn(ids);
      log.info("Deleted old jobs [{}].", StringUtils.join(ids, ','));

      jobExecutionService.deleteJobs(jobs);
    }
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
