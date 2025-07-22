package org.folio.des.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.JobDeletionIntervalEntity;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.delete_interval.JobDeletionInterval;
import org.folio.des.domain.dto.delete_interval.JobDeletionIntervalCollection;
import org.folio.des.domain.dto.delete_interval.Metadata;
import org.folio.des.repository.JobDeletionIntervalRepository;
import org.folio.des.service.JobDeletionIntervalService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class JobDeletionIntervalServiceImpl implements JobDeletionIntervalService {
  public static final String CREATED_BY_SYSTEM = "1c49bbf9-abda-4c02-a7cc-0504f8301268";

  private final JobDeletionIntervalRepository repository;
  private final FolioExecutionContext context;

  @Override
  public JobDeletionIntervalCollection getAll() {
    var entities = repository.findAll();
    var intervals = entities.stream()
      .map(this::mapToDto)
      .collect(Collectors.toList());
    return new JobDeletionIntervalCollection()
      .jobDeletionIntervals(intervals)
      .totalRecords(intervals.size());
  }

  @Override
  public JobDeletionInterval get(ExportType exportType) {
    return repository.findById(exportType)
      .map(this::mapToDto)
      .orElseThrow(() -> new NotFoundException("Interval for export type " + exportType + " not found"));
  }

  @Override
  public JobDeletionInterval create(JobDeletionInterval interval) {
    log.info("create:: Creating job deletion interval: {}", interval);
    if (repository.existsById(interval.getExportType())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
        "Interval for export type " + interval.getExportType() + " already exists");
    }
    var entity = mapToEntityForCreate(interval);
    return mapToDto(repository.save(entity));
  }

  @Override
  public JobDeletionInterval update(JobDeletionInterval interval) {
    log.info("update:: Updating job deletion interval: {}", interval);
    if (!repository.existsById(interval.getExportType())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        "Interval for export type " + interval.getExportType() + " not found");
    }
    var entity = mapToEntityForUpdate(interval);
    return mapToDto(repository.save(entity));
  }

  @Override
  public void delete(ExportType exportType) {
    log.info("delete:: Deleting interval for export type: {}", exportType);
    repository.deleteById(exportType);
  }

  private JobDeletionInterval mapToDto(JobDeletionIntervalEntity entity) {
    JobDeletionInterval interval = new JobDeletionInterval()
      .exportType(entity.getExportType())
      .retentionDays(entity.getRetentionDays());

    Metadata metadata = new Metadata();
    metadata.setCreatedDate(entity.getCreatedDate());
    metadata.setCreatedByUserId(entity.getCreatedBy());
    metadata.setUpdatedDate(entity.getUpdatedDate());
    metadata.setUpdatedByUserId(entity.getUpdatedBy());

    interval.setMetadata(metadata);
    return interval;
  }

  private JobDeletionIntervalEntity mapToEntityForUpdate(JobDeletionInterval dto) {
    var entity = new JobDeletionIntervalEntity();
    entity.setExportType(dto.getExportType());
    entity.setRetentionDays(dto.getRetentionDays());
    entity.setCreatedDate(dto.getMetadata().getCreatedDate());
    entity.setUpdatedBy(context.getUserId());
    entity.setUpdatedDate(LocalDateTime.now());
    if (dto.getMetadata() != null) {
      entity.setCreatedDate(dto.getMetadata().getCreatedDate());
      entity.setCreatedBy(dto.getMetadata().getCreatedByUserId());
    }
    return entity;
  }

  private JobDeletionIntervalEntity mapToEntityForCreate(JobDeletionInterval dto) {
    var entity = new JobDeletionIntervalEntity();
    entity.setExportType(dto.getExportType());
    entity.setRetentionDays(dto.getRetentionDays());
    entity.setCreatedBy(context.getUserId());
    entity.setCreatedDate(LocalDateTime.now());
    return entity;
  }
}
