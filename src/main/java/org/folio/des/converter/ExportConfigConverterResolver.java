package org.folio.des.converter;

import java.util.Map;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ModelConfiguration;
import org.springframework.core.convert.converter.Converter;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
public class ExportConfigConverterResolver {
  private final Map<ExportType, Converter<ExportConfig, ModelConfiguration>> converters;
  private final Converter<ExportConfig, ModelConfiguration> defaultConverter;

  public Converter<ExportConfig, ModelConfiguration> resolve(ExportType exportType) {
    return converters.getOrDefault(exportType, defaultConverter);
  }
}
