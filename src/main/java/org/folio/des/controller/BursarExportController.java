package org.folio.des.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.BursarExportConfig;
import org.folio.des.domain.dto.BursarExportConfigCollection;
import org.folio.des.rest.resource.BursarExportApi;
import org.folio.des.service.ConfigBursarExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/")
public class BursarExportController implements BursarExportApi {

  private final ConfigBursarExportService configService;

  @Override
  public ResponseEntity<BursarExportConfigCollection> getBursarExportConfig() {
    var configs = configService.getConfigCollection();
    return new ResponseEntity<>(configs, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<String> postBursarExportConfig(
      @Valid BursarExportConfig scheduleConfiguration) {
    configService.postConfig(scheduleConfiguration);

    return ResponseEntity.ok("Schedule configuration added");
  }

  @Override
  public ResponseEntity<String> putBursarExportConfig(
      String configId, @Valid BursarExportConfig config) {

    configService.updateConfig(configId, config);
    return ResponseEntity.ok("Schedule configuration updated");
  }
}
