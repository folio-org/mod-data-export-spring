package org.folio.des.converter;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Service
public final class DefaultModelConfigToExportConfigConverter implements Converter<ModelConfiguration, ExportConfig> {
  private final ObjectMapper objectMapper;

  @SneakyThrows
  @Override
  public ExportConfig convert(ModelConfiguration source) {
    final String value = source.getValue();
    var config = objectMapper.readValue(value, ExportConfig.class);
    config.setId(source.getId());
    return config;
  }
}
