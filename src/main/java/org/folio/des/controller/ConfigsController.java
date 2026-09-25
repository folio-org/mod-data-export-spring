package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportConfigExecuteRequest;
import org.folio.des.domain.dto.ExportConfigExecuteResponse;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.exception.ErrorCodes;
import org.folio.des.domain.exception.RequestValidationException;
import org.folio.des.rest.resource.ConfigsApi;
import org.folio.des.service.JobService;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Optional;

import static org.folio.des.domain.dto.ExportType.EDIFACT_ORDERS_EXPORT;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/data-export-spring")
public class ConfigsController implements ConfigsApi {

  private final ExportTypeBasedConfigManager manager;
  private final JobService jobService;

  @Override
  public ResponseEntity<ExportConfigCollection> getExportConfigs(String query, Integer limit) {
    log.info("getExportConfigs:: by query={} with limit={}", query, limit);
    return ResponseEntity.ok(manager.getConfigCollection(query, limit));
  }

  @Override
  public ResponseEntity<String> postExportConfig(@RequestHeader("X-Okapi-Tenant") String tenantId, ExportConfig exportConfig) {
    exportConfig.setTenant(tenantId);
    log.info("postExportConfig:: by exportConfig={}", exportConfig);
    manager.postConfig(exportConfig);
    return new ResponseEntity<>("Export configuration added", HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> putExportConfig(String configId, @RequestHeader("X-Okapi-Tenant") String tenantId, ExportConfig exportConfig) {
    log.info("putExportConfig:: configId={}.", configId);
    exportConfig.setTenant(tenantId);
    manager.updateConfig(configId, exportConfig);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ExportConfig> getConfigById(String exportConfigId) {
    log.info("getConfigById:: by exportConfigId={}.", exportConfigId);
    return ResponseEntity.ok(manager.getConfigById(exportConfigId));
  }

  @Override
  public ResponseEntity<Void> deleteExportConfigById(String exportConfigId) {
    log.info("deleteExportConfigById:: by exportConfigId={}.", exportConfigId);
    manager.deleteConfigById(exportConfigId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ExportConfigExecuteResponse> executeExportConfig(String exportConfigId, ExportConfigExecuteRequest executeRequest) {
    log.info("executeExportConfig:: by exportConfigId={} for poLineIds={}.", exportConfigId, executeRequest.getPoLineIds());
    var exportConfig = manager.getConfigById(exportConfigId);
    var ediConfig = getExecutableEdiConfig(exportConfig);
    ediConfig.setPoLineIds(executeRequest.getPoLineIds());
    var job = jobService.upsertAndSendToKafka(createJob(exportConfig), true);
    log.info("executeExportConfig:: configuration {} executed as jobId={}.", exportConfigId, job.getId());
    return new ResponseEntity<>(new ExportConfigExecuteResponse().jobId(String.valueOf(job.getId())), HttpStatus.CREATED);
  }

  /**
   * Only an Ordering EDIFACT orders export configuration selects PO lines, so it is the only
   * configuration that can be run for a caller-provided list of PO line ids.
   */
  private VendorEdiOrdersExportConfig getExecutableEdiConfig(ExportConfig exportConfig) {
    var ediConfig = Optional.ofNullable(exportConfig.getExportTypeSpecificParameters())
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .orElse(null);
    if (exportConfig.getType() != EDIFACT_ORDERS_EXPORT || ediConfig == null
        || ediConfig.getIntegrationType() != VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING) {
      log.warn("getExecutableEdiConfig:: configuration {} of type {} cannot be run manually.", exportConfig.getId(), exportConfig.getType());
      throw new RequestValidationException(ErrorCodes.EXPORT_CONFIGURATION_NOT_EXECUTABLE);
    }
    return ediConfig;
  }

  /**
   * {@code isSystemSource} and the creator fields are deliberately left unset: {@code JobService}
   * derives them from the request context, so the job is attributed to the user who triggered it
   * (unlike {@link org.folio.des.scheduling.quartz.job.acquisition.EdifactJob}, which runs
   * unattended and marks its jobs as system source).
   */
  private Job createJob(ExportConfig exportConfig) {
    var job = new Job();
    job.setType(exportConfig.getType());
    job.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
    job.setTenant(exportConfig.getTenant());
    return job;
  }
}
