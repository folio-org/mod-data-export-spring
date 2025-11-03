package org.folio.des.mapper;

import java.util.Map;

import org.folio.des.domain.dto.ExportType;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
public class ExportConfigMapperResolver {
  private final Map<ExportType, BaseExportConfigMapper> mappers;
  private final DefaultExportConfigMapper defaultMapper;

  public BaseExportConfigMapper resolve(ExportType exportType) {
    return mappers.getOrDefault(exportType, defaultMapper);
  }
}
