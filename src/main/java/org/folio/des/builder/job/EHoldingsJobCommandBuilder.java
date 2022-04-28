package org.folio.des.builder.job;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;

import org.folio.de.entity.Job;
import org.folio.des.domain.dto.EHoldingsExportConfig;

@Service
@Log4j2
@RequiredArgsConstructor
public class EHoldingsJobCommandBuilder implements JobCommandBuilder {
  private final ObjectMapper objectMapper;

  @Override
  public JobParameters buildJobCommand(Job job) {
    Map<String, JobParameter> params = new HashMap<>();
    EHoldingsExportConfig eHoldingsExportConfig = job.getExportTypeSpecificParameters().geteHoldingsExportConfig();
    try {
      params.put("recordId", new JobParameter(eHoldingsExportConfig.getRecordId()));
      params.put("recordType", new JobParameter(eHoldingsExportConfig.getRecordType().getValue()));
      params.put("titleSearchFilters", new JobParameter(eHoldingsExportConfig.getTitleSearchFilters()));
      params.put("packageFields", new JobParameter(objectMapper.writeValueAsString(eHoldingsExportConfig.getPackageFields())));
      params.put("titleFields", new JobParameter(objectMapper.writeValueAsString(eHoldingsExportConfig.getTitleFields())));
      return new JobParameters(params);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
