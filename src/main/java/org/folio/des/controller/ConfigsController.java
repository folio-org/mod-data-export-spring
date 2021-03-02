package org.folio.des.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.rest.resource.ConfigsApi;
import org.folio.des.service.ExportConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/")
public class ConfigsController implements ConfigsApi {

  private final ExportConfigService configService;

  @Override
  public ResponseEntity<ExportConfigCollection> getExportConfigs() {
    var configs = configService.getConfigCollection();
    return new ResponseEntity<>(configs, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<String> postExportConfig(@Valid ExportConfig exportConfig) {
    configService.postConfig(exportConfig);
    return ResponseEntity.ok("Export configuration added");
  }

  @Override
  public ResponseEntity<String> putExportConfig(String configId, @Valid ExportConfig exportConfig) {
    configService.updateConfig(configId, exportConfig);
    return ResponseEntity.ok("Export configuration updated");
  }

}
