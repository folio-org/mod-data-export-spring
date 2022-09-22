package org.folio.des.builder.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.Job;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class EdifactOrdersJobCommandBuilder implements JobCommandBuilder {
  private final ObjectMapper objectMapper;
  private final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  private final ConfigurationClient manager;


  @Override
  public JobParameters buildJobCommand(Job job) {
    Map<String, JobParameter> params = new HashMap<>();
    ModelConfiguration modelConfiguration = manager.getConfigById(job.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getExportConfigId().toString());
    ExportConfig config = defaultModelConfigToExportConfigConverter.convert(modelConfiguration);

    try {
      params.put("edifactOrdersExport",
        new JobParameter(objectMapper.writeValueAsString(job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig())));
      params.put("fileName",new JobParameter(objectMapper.writeValueAsString(job.getFileNames())));
      params.put("ediFtp",new JobParameter(objectMapper.writeValueAsString(config.getExportTypeSpecificParameters()
        .getVendorEdiOrdersExportConfig()
        .getEdiFtp())));
      return new JobParameters(params);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
