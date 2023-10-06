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

  public static org.folio.des.domain.dto.Job entityToDto(Job jobEntity) {
    var result = new org.folio.des.domain.dto.Job();

    result.setId(jobEntity.getId());
    result.setName(jobEntity.getName());
    result.setDescription(jobEntity.getDescription());
    result.setSource(jobEntity.getSource());
    result.setIsSystemSource(jobEntity.getIsSystemSource());
    result.setType(jobEntity.getType());
    result.setExportTypeSpecificParameters(
      jobEntity.getExportTypeSpecificParameters()
    );
    result.setStatus(jobEntity.getStatus());
    if (
      ObjectUtils.notEqual(
        ExportType.EDIFACT_ORDERS_EXPORT,
        jobEntity.getType()
      )
    ) {
      result.setFiles(jobEntity.getFiles());
    }
    result.setFileNames(jobEntity.getFileNames());
    result.setStartTime(jobEntity.getStartTime());
    result.setEndTime(jobEntity.getEndTime());
    result.setIdentifierType(jobEntity.getIdentifierType());
    result.setEntityType(jobEntity.getEntityType());
    result.setProgress(jobEntity.getProgress());

    var metadata = new Metadata();
    metadata.setCreatedDate(jobEntity.getCreatedDate());
    metadata.setCreatedByUserId(jobEntity.getCreatedByUserId());
    metadata.setCreatedByUsername(jobEntity.getCreatedByUsername());
    metadata.setUpdatedDate(jobEntity.getUpdatedDate());
    metadata.setUpdatedByUserId(jobEntity.getUpdatedByUserId());
    metadata.setUpdatedByUsername(jobEntity.getUpdatedByUsername());
    result.setMetadata(metadata);

    result.setOutputFormat(jobEntity.getOutputFormat());
    result.setErrorDetails(jobEntity.getErrorDetails());

    return result;
  }

  public static org.folio.des.domain.dto.LegacyJob entityToDto(
    LegacyJob legacyJobEntity
  ) {
    var result = new org.folio.des.domain.dto.LegacyJob();

    result.setId(legacyJobEntity.getId());
    result.setName(legacyJobEntity.getName());
    result.setDescription(legacyJobEntity.getDescription());
    result.setSource(legacyJobEntity.getSource());
    result.setIsSystemSource(legacyJobEntity.getIsSystemSource());
    result.setType(legacyJobEntity.getType());
    result.setExportTypeSpecificParameters(
      legacyJobEntity.getExportTypeSpecificParameters()
    );
    result.setStatus(legacyJobEntity.getStatus());
    if (
      ObjectUtils.notEqual(
        ExportType.EDIFACT_ORDERS_EXPORT,
        legacyJobEntity.getType()
      )
    ) {
      result.setFiles(legacyJobEntity.getFiles());
    }
    result.setFileNames(legacyJobEntity.getFileNames());
    result.setStartTime(legacyJobEntity.getStartTime());
    result.setEndTime(legacyJobEntity.getEndTime());
    result.setIdentifierType(legacyJobEntity.getIdentifierType());
    result.setEntityType(legacyJobEntity.getEntityType());
    result.setProgress(legacyJobEntity.getProgress());

    var metadata = new Metadata();
    metadata.setCreatedDate(legacyJobEntity.getCreatedDate());
    metadata.setCreatedByUserId(legacyJobEntity.getCreatedByUserId());
    metadata.setCreatedByUsername(legacyJobEntity.getCreatedByUsername());
    metadata.setUpdatedDate(legacyJobEntity.getUpdatedDate());
    metadata.setUpdatedByUserId(legacyJobEntity.getUpdatedByUserId());
    metadata.setUpdatedByUsername(legacyJobEntity.getUpdatedByUsername());
    result.setMetadata(metadata);

    result.setOutputFormat(legacyJobEntity.getOutputFormat());
    result.setErrorDetails(legacyJobEntity.getErrorDetails());

    return result;
  }
}
