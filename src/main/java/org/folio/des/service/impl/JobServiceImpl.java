package org.folio.des.service.impl;

import static java.util.Objects.nonNull;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.Job;
import org.folio.des.client.ExportWorkerClient;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.dto.PresignedUrl;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.delete_interval.JobDeletionInterval;
import org.folio.des.exceptions.FileDownloadException;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.security.JWTokenUtils;
import org.folio.des.service.JobDeletionIntervalService;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.util.JobMapperUtil;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableScheduling
@Log4j2
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

  @Value("${job.expirationPeriodDays}")
  private int jobExpirationPeriod;
  @Value("${job.downloadFileConnectionTimeoutMs}")
  private int jobDownloadFileConnectionTimeout;

  private static final Map<ExportType, String> OUTPUT_FORMATS = new EnumMap<>(ExportType.class);

  static {
    OUTPUT_FORMATS.put(ExportType.BURSAR_FEES_FINES, "Fees & Fines Bursar Report");
    OUTPUT_FORMATS.put(ExportType.CIRCULATION_LOG, "Comma-Separated Values (CSV)");
    OUTPUT_FORMATS.put(ExportType.EDIFACT_ORDERS_EXPORT, "EDIFACT orders export (EDI)");
  }

  private final ExportWorkerClient exportWorkerClient;
  private final JobExecutionService jobExecutionService;
  private final JobDataExportRepository repository;
  private final FolioExecutionContext context;
  private final CQLService cqlService;
  private final JobDeletionIntervalService deletionIntervalService;
  private final ExportConfigService defaultExportConfigService;

  @Transactional(readOnly = true)
  @Override
  public org.folio.des.domain.dto.Job get(UUID id) {
    log.debug("get:: by id={}.", id);
    return entityToDto(getJobEntity(id));
  }

  public Job getJobEntity(UUID id) {
    log.debug("getJobEntity:: by id={}.", id);
    return repository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Job %s not found", id)));
  }

  @Transactional(readOnly = true)
  @Override
  public JobCollection get(Integer offset, Integer limit, String query) {
    log.debug("get:: by query={} with offset={} and limit={}.", query, offset, limit);
    var result = new JobCollection();
    if (StringUtils.isBlank(query)) {
      log.info("get:: get all since query is absent.");
      Page<Job> page = repository.findAll(new OffsetRequest(offset, limit));
      result.setJobRecords(page.map(JobServiceImpl::entityToDto).getContent());
      result.setTotalRecords((int) page.getTotalElements());
    } else {
      result.setJobRecords(cqlService.getByCQL(Job.class, query, offset, limit)
          .stream()
          .map(JobServiceImpl::entityToDto)
          .toList());
      result.setTotalRecords(cqlService.countByCQL(Job.class, query));
    }
    log.info("get:: result={}", result);
    return result;
  }

  @Transactional
  @Override
  public org.folio.des.domain.dto.Job upsertAndSendToKafka(org.folio.des.domain.dto.Job jobDto, boolean withJobCommandSend) {
    return upsertAndSendToKafka(jobDto, withJobCommandSend, true);
  }

  @Transactional
  @Override
  public org.folio.des.domain.dto.Job upsertAndSendToKafka(org.folio.des.domain.dto.Job jobDto, boolean withJobCommandSend,
                                                           boolean validateConfigPresence) {
    log.info("upsertAndSendToKafka:: with jobDto={} and withJobCommandSend={} and validateConfigPresence={}.",
                                                          jobDto, withJobCommandSend, validateConfigPresence);

    if (validateConfigPresence) {
      log.info("upsertAndSendToKafka:: validate config presence for job id {}", jobDto.getId());
      Optional.ofNullable(jobDto.getExportTypeSpecificParameters())
        .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
        .map(VendorEdiOrdersExportConfig::getExportConfigId).ifPresent(configId -> {
          try {
            defaultExportConfigService.getConfigById(configId.toString());
          } catch (NotFoundException e) {
            log.error("Config with id {} not found", configId.toString());
            jobDto.getExportTypeSpecificParameters()
              .getVendorEdiOrdersExportConfig()
              .getEdiSchedule()
              .getScheduleParameters()
              .setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);

            throw e;
          }
        });
    }
    Job result = dtoToEntity(jobDto);

    if (StringUtils.isBlank(result.getName())) {
      result.setName(String.format("%06d", repository.getNextJobNumber()));
    }
    String userName = getUserName(context);
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
    UUID userId = context.getUserId();
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

    result = repository.save(result);
    log.info("upsertAndSendToKafka:: job by id  {} was upserted.", result.getId());

    if (withJobCommandSend) {
      var jobCommand = jobExecutionService.prepareStartJobCommand(result);
      jobExecutionService.sendJobCommand(jobCommand);
    }

    return entityToDto(result);
  }

  @Transactional
  @Override
  public void deleteOldJobs() {
    List<JobDeletionInterval> jobDeletionIntervals = deletionIntervalService.getAll().getJobDeletionIntervals();
    if (CollectionUtils.isEmpty(jobDeletionIntervals)) {
      log.info("deleteOldJobs:: No job deletion intervals found, skipping.");
      return;
    }
    List<Job> jobsToDelete = new ArrayList<>();

    jobDeletionIntervals.forEach(interval -> {
      log.info("deleteOldJobs:: Deleting old jobs for export type: {} with retention days: {}.", interval.getExportType(), interval.getRetentionDays());
      var expirationDate = createExpirationDate(interval.getRetentionDays());
      List<Job> expiredJobs = repository.findByTypeAndUpdatedDateBefore(interval.getExportType(), expirationDate);
      log.info("deleteOldJobs:: Found {} old jobs for export type: {} for deletion.", expiredJobs.size(), interval.getExportType());
      jobsToDelete.addAll(expiredJobs);
    });

    deleteJobs(jobsToDelete);
  }

  @Transactional
  @Override
  public void resendExportedFile(UUID jobId) {
    log.info("resendExportedFile:: resend exported files for job with jobId={}.", jobId);
    org.folio.des.domain.dto.Job job = get(jobId);
    if (CollectionUtils.isEmpty(job.getFileNames())) {
      log.error("The exported file is missing for jobId={}.", job.getId());
      throw new NotFoundException(String.format("The exported file is missing for jobId: %s", job.getId()));
    }
    var jobCommand = jobExecutionService.prepareResendJobCommand(dtoToEntity(job));
    jobExecutionService.sendJobCommand(jobCommand);
  }

  private Date createExpirationDate(int days) {
    return Date.from(LocalDate.now().minusDays(days).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
  }

  public void deleteJobs(List<Job> jobs) {
    if (CollectionUtils.isEmpty(jobs)) {
      log.info("deleteJobs:: No old jobs were found.");
      return;
    }

    repository.deleteAllInBatch(jobs);
    log.info("deleteJobs:: Deleted {} old jobs [{}].", jobs.size(), jobs.stream()
      .map(job -> String.format("ID: %s, Name: %s, Type: %s, Status: %s",
        job.getId(),
        job.getName(),
        job.getType(),
        job.getStatus()))
      .collect(Collectors.joining("; ")));

    jobExecutionService.deleteJobs(jobs);
  }

  @Override
  public InputStream downloadExportedFile(UUID jobId, String key) {
    log.debug("downloadExportedFile:: download exported files for jobId={}.", jobId);
    Job job = getJobEntity(jobId);
    if (CollectionUtils.isEmpty(job.getFileNames())) {
      log.error("The URL of the exported file is missing for jobId={}.", job.getId());
      throw new NotFoundException(String.format("The URL of the exported file is missing for jobId: %s", job.getId()));
    }
    log.debug("Refreshing download url for jobId: {}", job.getId());
    try {
      PresignedUrl presignedUrl = getPresignedUrl(job, key);
      URL url = new URL(presignedUrl.getUrl());
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(jobDownloadFileConnectionTimeout);
      return conn.getInputStream();
    } catch (Exception e) {
      log.error("Error downloading a file: {} for jobId: {} and key: {}", e.getMessage(), job.getId(), key);
      throw new FileDownloadException(String.format("Error downloading a file: %s", e));
    }
  }

  public static org.folio.des.domain.dto.Job entityToDto(Job entity) {
    return JobMapperUtil.entityToDto(entity);
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
    result.setFileNames(dto.getFileNames());

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

  private String getUserName(FolioExecutionContext context) {
    String jwt = context.getToken();
    Optional<JWTokenUtils.UserInfo> userInfo = StringUtils.isBlank(jwt) ? Optional.empty() : JWTokenUtils.parseToken(jwt);
    return StringUtils.substring(userInfo.map(JWTokenUtils.UserInfo::getUserName).orElse(null), 0, 50);
  }

  private PresignedUrl getPresignedUrl(Job job, String key) {
    if (nonNull(key)) {
      return exportWorkerClient.getRefreshedPresignedUrl(key);
    }
    return exportWorkerClient.getRefreshedPresignedUrl(job.getFiles().get(0));
  }
}
