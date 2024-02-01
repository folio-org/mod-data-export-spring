package org.folio.des.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.ModelConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class LoggerUtils {

  private final static ObjectMapper objectMapper = new ObjectMapper();

  private LoggerUtils() {}

  public static ExportConfigCollection getExportConfigCollectionForLog(ExportConfigCollection configCollection) {
    try {
      ExportConfigCollection configCollectionDeepCopy = objectMapper.readValue(objectMapper.writeValueAsString(configCollection), ExportConfigCollection.class);
      List<ExportConfig> configList = configCollectionDeepCopy.getConfigs()
        .stream()
        .map(LoggerUtils::getExportConfigForLog)
        .toList();
      return configCollectionDeepCopy.configs(configList);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static ExportConfig getExportConfigForLog(ExportConfig exportConfig) {
    try {
      ExportConfig configDeepCopy = objectMapper.readValue(objectMapper.writeValueAsString(exportConfig), ExportConfig.class);
      ExportTypeSpecificParameters exportTypeSpecificParameters = configDeepCopy.getExportTypeSpecificParameters();
      updateEdiFtp(exportTypeSpecificParameters);
      return configDeepCopy;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void updateEdiFtp(ExportTypeSpecificParameters exportTypeSpecificParameters) {
    if (Objects.nonNull(exportTypeSpecificParameters) &&
      Objects.nonNull(exportTypeSpecificParameters.getVendorEdiOrdersExportConfig()) &&
      Objects.nonNull(exportTypeSpecificParameters.getVendorEdiOrdersExportConfig().getEdiFtp())) {
      EdiFtp ediFtp = exportTypeSpecificParameters.getVendorEdiOrdersExportConfig().getEdiFtp();
      ediFtp.setServerAddress(null);
      ediFtp.setUsername(null);
      ediFtp.setPassword(null);
      ediFtp.ftpPort(null);
    }
  }

  public static ModelConfiguration getModelConfigurationForLog(ModelConfiguration modelConfiguration, ExportConfig exportConfigForLog) {
    try {
      ModelConfiguration modelConfigurationDeepCopy = objectMapper.readValue(objectMapper.writeValueAsString(modelConfiguration), ModelConfiguration.class);
      return modelConfigurationDeepCopy.value(objectMapper.writeValueAsString(exportConfigForLog));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static Job getJobForLog(Job job) {
    try {
      Job jobDeepCopy = objectMapper.readValue(objectMapper.writeValueAsString(job), Job.class);
      ExportTypeSpecificParameters exportTypeSpecificParameters = job.getExportTypeSpecificParameters();
      updateEdiFtp(exportTypeSpecificParameters);
      return jobDeepCopy;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static JobCollection getJobCollectionForLog(JobCollection jobCollection) {
    try {
      JobCollection jobCollectionDeepCopy = objectMapper.readValue(objectMapper.writeValueAsString(jobCollection), JobCollection.class);
      List<Job> jobList = jobCollectionDeepCopy.getJobRecords()
        .stream()
        .map(LoggerUtils::getJobForLog)
        .toList();
      return jobCollectionDeepCopy.jobRecords(jobList);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static org.folio.de.entity.Job getJobForLog(org.folio.de.entity.Job job) {
    try {
      org.folio.de.entity.Job jobDeepCopy = objectMapper.readValue(objectMapper.writeValueAsString(job), org.folio.de.entity.Job.class);
      ExportTypeSpecificParameters exportTypeSpecificParameters = job.getExportTypeSpecificParameters();
      updateEdiFtp(exportTypeSpecificParameters);
      return jobDeepCopy;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
