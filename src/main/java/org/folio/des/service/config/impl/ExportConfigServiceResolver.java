package org.folio.des.service.config.impl;

import java.util.Map;
import java.util.Optional;

import org.folio.des.domain.dto.ExportType;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.service.config.ExportConfigService;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Log4j2
public class ExportConfigServiceResolver {

  private final Map<ExportType, ExportConfigService> exportConfigServiceMap;

  public Optional<ExportConfigService> resolve(ExportType exportType) {
    return Optional.ofNullable(exportType).map(expType -> exportConfigServiceMap.get(expType));
  }
}
