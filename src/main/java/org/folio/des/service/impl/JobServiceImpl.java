package org.folio.des.service.impl;

import static org.folio.des.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.des.domain.dto.ExportType.BULK_EDIT_QUERY;
import static org.folio.des.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.dto.Metadata;
import org.folio.de.entity.Job;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;

@Service
@EnableScheduling
@Log4j2
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
  private static final int DEFAULT_JOB_EXPIRATION_PERIOD = 7;
  public static final String INTEGRATION_NOT_AVAILABLE = "Integration not available";

  private static final Map<ExportType, String> OUTPUT_FORMATS = new EnumMap<>(ExportType.class);

  static {
    OUTPUT_FORMATS.put(ExportType.BURSAR_FEES_FINES, "Fees & Fines Bursar Report");
    OUTPUT_FORMATS.put(ExportType.CIRCULATION_LOG, "Comma-Separated Values (CSV)");
    OUTPUT_FORMATS.put(ExportType.EDIFACT_ORDERS_EXPORT, "EDIFACT orders export (EDI)");
  }

  private final JobExecutionService jobExecutionService;
  private final JobDataExportRepository repository;
  private final FolioExecutionContext context;
  private final CQLService cqlService;
  private final BulkEditConfigService bulkEditConfigService;
  private Set<ExportType> bulkEditTypes = Set.of(BULK_EDIT_IDENTIFIERS, BULK_EDIT_QUERY, BULK_EDIT_UPDATE);
  private final ExportTypeBasedConfigManager exportTypeBasedConfigManager;

  @Transactional(readOnly = true)
  @Override
  public org.folio.des.domain.dto.Job get(UUID id) {
    Optional<Job> jobDtoOptional = repository.findById(id);
    if (jobDtoOptional.isEmpty()) {
      throw new NotFoundException(String.format("Job %s not found", id));
    }
    return entityToDto(jobDtoOptional.get());
  }

  @Transactional(readOnly = true)
  @Override
  public JobCollection get(Integer offset, Integer limit, String query) {
    var result = new JobCollection();
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

  @Transactional
  @Override
  public org.folio.des.domain.dto.Job upsertAndSendToKafka(org.folio.des.domain.dto.Job jobDto, boolean withJobCommandSend) {

     Optional.ofNullable(jobDto.getExportTypeSpecificParameters()).ifPresent(
       f -> Optional.ofNullable(f.getVendorEdiOrdersExportConfig()).ifPresent(f1->
         Optional.ofNullable(f1.getExportConfigId())
           .ifPresent(s -> {
           try {
               log.info("Looking config with id {}", s.toString());
               exportTypeBasedConfigManager.getConfigById(s.toString());
             } catch (NotFoundException e) {
               log.info("config not found", f.getVendorEdiOrdersExportConfig().getExportConfigId().toString());
               throw new NotFoundException(String.format(INTEGRATION_NOT_AVAILABLE, s));
             }
           })
       ));
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
    var now = new Date();
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

    log.info("Upserting {}.", result);
    result = repository.save(result);
    log.info("Upserted {}.", result);

    if (withJobCommandSend) {
      var jobCommand = jobExecutionService.prepareStartJobCommand(result);
      jobExecutionService.sendJobCommand(jobCommand);
    }

    return entityToDto(result);
  }

  @Transactional
  @Override
  public void deleteOldJobs() {
    var expirationDate = createExpirationDate(DEFAULT_JOB_EXPIRATION_PERIOD);
    log.info("Collecting old jobs with 'updatedDate' less than {}.", expirationDate);
    var jobsToDelete = filterJobsNotMatchingExportTypes(repository.findByUpdatedDateBefore(expirationDate), bulkEditTypes);

    expirationDate = createExpirationDate(bulkEditConfigService.getBulkEditJobExpirationPeriod());
    log.info("Collecting old bulk-edit jobs with 'updatedDate' less than {}.", expirationDate);
    jobsToDelete.addAll(filterJobsMatchingExportTypes(repository.findByUpdatedDateBefore(expirationDate), bulkEditTypes));

    deleteJobs(jobsToDelete);
  }

  private Date createExpirationDate(int days) {
    return Date.from(LocalDate.now().minusDays(days).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
  }

  private List<Job> filterJobsNotMatchingExportTypes(List<Job> jobs, Set<ExportType> exportTypes) {
    return jobs.stream()
      .filter(j -> !exportTypes.contains(j.getType()))
      .collect(Collectors.toList());
  }

  private List<Job> filterJobsMatchingExportTypes(List<Job> jobs, Set<ExportType> exportTypes) {
    return jobs.stream()
      .filter(j -> exportTypes.contains(j.getType()))
      .collect(Collectors.toList());
  }

  public void deleteJobs(List<Job> jobs) {
    if (CollectionUtils.isEmpty(jobs)) {
      log.info("No old jobs were found.");
      return;
    }

    repository.deleteAllInBatch(jobs);
    log.info("Deleted old jobs [{}].", StringUtils.join(jobs, ','));

    jobExecutionService.deleteJobs(jobs);
  }

  public static org.folio.des.domain.dto.Job entityToDto(Job entity) {
    var result = new org.folio.des.domain.dto.Job();

    result.setId(entity.getId());
    result.setName(entity.getName());
    result.setDescription(entity.getDescription());
    result.setSource(entity.getSource());
    result.setIsSystemSource(entity.getIsSystemSource());
    result.setType(entity.getType());
    result.setExportTypeSpecificParameters(entity.getExportTypeSpecificParameters());
    result.setStatus(entity.getStatus());
    result.setFiles(entity.getFiles());
    result.setFileNames(entity.getFileNames());
    result.setStartTime(entity.getStartTime());
    result.setEndTime(entity.getEndTime());
    result.setIdentifierType(entity.getIdentifierType());
    result.setEntityType(entity.getEntityType());
    result.setProgress(entity.getProgress());

    var metadata = new Metadata();
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
    var result = new Job();

    result.setId(dto.getId());
    result.setName(dto.getName());
    result.setDescription(dto.getDescription());
    result.setSource(dto.getSource());
    result.setIsSystemSource(dto.getIsSystemSource());
    result.setType(dto.getType());
    result.setExportTypeSpecificParameters(dto.getExportTypeSpecificParameters());
    result.setStatus(dto.getStatus());
    result.setFiles(dto.getFiles());
    result.setStartTime(dto.getStartTime());
    result.setEndTime(dto.getEndTime());
    result.setIdentifierType(dto.getIdentifierType());
    result.setEntityType(dto.getEntityType());
    result.setProgress(dto.getProgress());

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
