package org.folio.des.service.util;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.de.entity.Job;
import org.folio.de.entity.bursarlegacy.LegacyJob;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.Metadata;

public class JobMapperUtil {

  private JobMapperUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static org.folio.des.domain.dto.Job entityToDto(Job entity) {
    var result = new org.folio.des.domain.dto.Job();

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

  public static org.folio.des.domain.dto.LegacyJob entityToDto(
    LegacyJob entity
  ) {
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
