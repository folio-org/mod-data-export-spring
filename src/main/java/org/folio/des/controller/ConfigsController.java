package org.folio.des.controller;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.rest.resource.ConfigsApi;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/data-export-spring")
@RequiredArgsConstructor
public class ConfigsController implements ConfigsApi {

 // private final ExportTypeBasedConfigManager manager;

//  @Override
//  public ResponseEntity<ExportConfigCollection> getExportConfigs(ExportType exportType, String query) {
//      return ResponseEntity.ok(manager.getConfigCollection(exportType, query));
//  }
//
//  @Override
//  public ResponseEntity<String> postExportConfig(ExportConfig exportConfig) {
//    manager.postConfig(exportConfig);
//    return new ResponseEntity<>("Export configuration added", HttpStatus.CREATED);
//  }
//
//  @Override
//  public ResponseEntity<Void> putExportConfig(String configId, ExportConfig exportConfig) {
//    manager.updateConfig(configId, exportConfig);
//    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//  }
//
//  @Override
//  public ResponseEntity<ExportConfig> getConfigById(String id) {
//    return ResponseEntity.ok(manager.getConfigById(id));
//  }
}
