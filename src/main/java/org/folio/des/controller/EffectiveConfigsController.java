package org.folio.des.controller;

import java.util.UUID;

import org.folio.des.domain.dto.EffectiveConfigType;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.TransmissionMethod;
import org.folio.des.rest.resource.EffectiveConfigsApi;
import org.folio.des.service.config.acquisition.ConfigCriteria;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/data-export-spring")
public class EffectiveConfigsController implements EffectiveConfigsApi {

  private final ExportTypeBasedConfigManager manager;

  @Override
  public ResponseEntity<ExportConfigCollection> getEffectiveConfigs(UUID vendorId, EffectiveConfigType type, UUID acquisitionMethod,
      String vendorAccount, TransmissionMethod transmissionMethod) {
    var criteria = new ConfigCriteria(vendorId, acquisitionMethod, vendorAccount, type, transmissionMethod);
    log.info("getEffectiveConfigs:: by criteria={}.", criteria);
    return ResponseEntity.ok(manager.getEffectiveConfigs(criteria));
  }

}
