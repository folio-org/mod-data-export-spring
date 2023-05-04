package org.folio.des.service.bursarlegacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.bursarlegacy.LegacyJob;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.LegacyJobCollection;
import org.folio.des.domain.dto.Metadata;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.bursarlegacy.BursarExportLegacyJobRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class BursarExportLegacyJobService {

  private final BursarExportLegacyJobRepository repository;

  private final CQLService cqlService;

  @Transactional(readOnly = true)
  public LegacyJobCollection get(Integer offset, Integer limit, String query) {
    var result = new LegacyJobCollection();
    if (StringUtils.isBlank(query)) {
      Page<LegacyJob> page = repository.findAll(
        new OffsetRequest(offset, limit)
      );
      result.setJobRecords(page.map(this::entityToDto).getContent());
      result.setTotalRecords((int) page.getTotalElements());
    } else {
      result.setJobRecords(
        cqlService
          .getByCQL(LegacyJob.class, query, offset, limit)
          .stream()
          .map(this::entityToDto)
          .toList()
      );
      result.setTotalRecords(cqlService.countByCQL(LegacyJob.class, query));
    }
    return result;
  }

  private org.folio.des.domain.dto.LegacyJob entityToDto(LegacyJob entity) {
    var result = new org.folio.des.domain.dto.LegacyJob();

    result.setId(entity.getId());
    result.setName(entity.getName());
    result.setDescription(entity.getDescription());
    result.setSource(entity.getSource());
    result.setIsSystemSource(entity.getIsSystemSource());
    result.setType(entity.getType());
    result.setExportTypeSpecificParameters(
      entity.getExportTypeSpecificParameters()
    );
    result.setStatus(entity.getStatus());
    if (
      ObjectUtils.notEqual(ExportType.EDIFACT_ORDERS_EXPORT, entity.getType())
    ) {
      result.setFiles(entity.getFiles());
    }
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
}
