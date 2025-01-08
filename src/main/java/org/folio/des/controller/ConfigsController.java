package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.rest.resource.ConfigsApi;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;

import static org.folio.des.domain.dto.ExportType.EDIFACT_ORDERS_EXPORT;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/data-export-spring")
public class ConfigsController implements ConfigsApi {

  private final EnumSet<ExportType> applyAspectExportTypes = EnumSet.of(EDIFACT_ORDERS_EXPORT);
  private final ExportTypeBasedConfigManager manager;

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
    if (applyAspectExportTypes.contains(exportConfig.getType())) {
      String config = exportConfig.toString();
      return new ResponseEntity<>(config.substring(config.indexOf("{")), HttpStatus.CREATED);
    }
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
}
