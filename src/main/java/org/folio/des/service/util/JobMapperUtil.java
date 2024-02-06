package org.folio.des.service.util;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.de.entity.Job;
import org.folio.de.entity.bursarlegacy.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Metadata;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JobMapperUtil {

  public static org.folio.des.domain.dto.Job entityToDto(Job jobEntity) {
    var result = new org.folio.des.domain.dto.Job();

    result.setId(jobEntity.getId());
    result.setName(jobEntity.getName());
    result.setDescription(jobEntity.getDescription());
    result.setSource(jobEntity.getSource());
    result.setIsSystemSource(jobEntity.getIsSystemSource());
    result.setType(jobEntity.getType());
    result.setExportTypeSpecificParameters(jobEntity.getExportTypeSpecificParameters());
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

  public static org.folio.des.domain.dto.JobWithLegacyBursarParameters entityToDto(
    JobWithLegacyBursarParameters jobWithLegacyBursarParametersEntity
  ) {
    var result = new org.folio.des.domain.dto.JobWithLegacyBursarParameters();

    result.setId(jobWithLegacyBursarParametersEntity.getId());
    result.setName(jobWithLegacyBursarParametersEntity.getName());
    result.setDescription(jobWithLegacyBursarParametersEntity.getDescription());
    result.setSource(jobWithLegacyBursarParametersEntity.getSource());
    result.setIsSystemSource(jobWithLegacyBursarParametersEntity.getIsSystemSource());
    result.setType(jobWithLegacyBursarParametersEntity.getType());
    result.setExportTypeSpecificParameters(jobWithLegacyBursarParametersEntity.getExportTypeSpecificParameters());
    result.setStatus(jobWithLegacyBursarParametersEntity.getStatus());
    if (
      ObjectUtils.notEqual(
        ExportType.EDIFACT_ORDERS_EXPORT,
        jobWithLegacyBursarParametersEntity.getType()
      )
    ) {
      result.setFiles(jobWithLegacyBursarParametersEntity.getFiles());
    }
    result.setFileNames(jobWithLegacyBursarParametersEntity.getFileNames());
    result.setStartTime(jobWithLegacyBursarParametersEntity.getStartTime());
    result.setEndTime(jobWithLegacyBursarParametersEntity.getEndTime());
    result.setIdentifierType(jobWithLegacyBursarParametersEntity.getIdentifierType());
    result.setEntityType(jobWithLegacyBursarParametersEntity.getEntityType());
    result.setProgress(jobWithLegacyBursarParametersEntity.getProgress());

    var metadata = new Metadata();
    metadata.setCreatedDate(jobWithLegacyBursarParametersEntity.getCreatedDate());
    metadata.setCreatedByUserId(jobWithLegacyBursarParametersEntity.getCreatedByUserId());
    metadata.setCreatedByUsername(jobWithLegacyBursarParametersEntity.getCreatedByUsername());
    metadata.setUpdatedDate(jobWithLegacyBursarParametersEntity.getUpdatedDate());
    metadata.setUpdatedByUserId(jobWithLegacyBursarParametersEntity.getUpdatedByUserId());
    metadata.setUpdatedByUsername(jobWithLegacyBursarParametersEntity.getUpdatedByUsername());
    result.setMetadata(metadata);

    result.setOutputFormat(jobWithLegacyBursarParametersEntity.getOutputFormat());
    result.setErrorDetails(jobWithLegacyBursarParametersEntity.getErrorDetails());

    return result;
  }

  public static org.folio.des.domain.dto.Job legacyBursarToNewDto(
    org.folio.des.domain.dto.JobWithLegacyBursarParameters jobWithLegacyBursarParametersEntity,
    ExportTypeSpecificParameters newExportTypeSpecificParameters
  ) {
    var result = new org.folio.des.domain.dto.Job();

    // this is the only different field between the two
    result.setExportTypeSpecificParameters(newExportTypeSpecificParameters);

    result.setId(jobWithLegacyBursarParametersEntity.getId());
    result.setName(jobWithLegacyBursarParametersEntity.getName());
    result.setDescription(jobWithLegacyBursarParametersEntity.getDescription());
    result.setSource(jobWithLegacyBursarParametersEntity.getSource());
    result.setIsSystemSource(jobWithLegacyBursarParametersEntity.getIsSystemSource());
    result.setTenant(jobWithLegacyBursarParametersEntity.getTenant());
    result.setType(jobWithLegacyBursarParametersEntity.getType());
    result.setStatus(jobWithLegacyBursarParametersEntity.getStatus());
    result.setFiles(jobWithLegacyBursarParametersEntity.getFiles());
    result.setFileNames(jobWithLegacyBursarParametersEntity.getFileNames());
    result.setStartTime(jobWithLegacyBursarParametersEntity.getStartTime());
    result.setEndTime(jobWithLegacyBursarParametersEntity.getEndTime());
    result.setIdentifierType(jobWithLegacyBursarParametersEntity.getIdentifierType());
    result.setEntityType(jobWithLegacyBursarParametersEntity.getEntityType());
    result.setProgress(jobWithLegacyBursarParametersEntity.getProgress());

    result.setMetadata(jobWithLegacyBursarParametersEntity.getMetadata());

    result.setOutputFormat(jobWithLegacyBursarParametersEntity.getOutputFormat());
    result.setErrorDetails(jobWithLegacyBursarParametersEntity.getErrorDetails());

    return result;
  }
}
