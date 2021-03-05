package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.rest.resource.ConfigsApi;
import org.folio.des.service.ExportConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/data-export-spring")
@RequiredArgsConstructor
public class ConfigsController implements ConfigsApi {

  private final ExportConfigService service;

  @Override
  public ResponseEntity<ExportConfigCollection> getExportConfigs() {
    return ResponseEntity.ok(service.getConfigCollection());
  }

  @Override
  public ResponseEntity<String> postExportConfig(@Valid ExportConfig exportConfig) {
    service.postConfig(exportConfig);
    return new ResponseEntity<>("Export configuration added", HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<String> putExportConfig(String configId, @Valid ExportConfig exportConfig) {
    service.updateConfig(configId, exportConfig);
    return ResponseEntity.ok("Export configuration updated");
  }

}
