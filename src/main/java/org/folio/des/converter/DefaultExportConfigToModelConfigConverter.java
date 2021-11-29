package org.folio.des.converter;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.MODULE_NAME;

@AllArgsConstructor
@Log4j2
@Service
public final class DefaultExportConfigToModelConfigConverter implements Converter<ExportConfig, ModelConfiguration> {
  private static final String CONFIG_DESCRIPTION = "Data export configuration parameters";

  private final ObjectMapper objectMapper;

  @SneakyThrows
  @Override
  public ModelConfiguration convert(ExportConfig source) {
    var config = new ModelConfiguration();
    config.setModule(MODULE_NAME);
    config.setConfigName(DEFAULT_CONFIG_NAME);
    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(source));
    return config;
  }
}
