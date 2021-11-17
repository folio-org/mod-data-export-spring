package org.folio.des.converter;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
public final class BursarFeesFinesExportConfigConverter implements Converter<ExportConfig, ModelConfiguration> {
  private static final String MODULE_NAME = "mod-data-export-spring";
  private static final String CONFIG_NAME = "export_config_parameters";
  private static final String CONFIG_DESCRIPTION = "Data export configuration parameters";

  private final ObjectMapper objectMapper;

  @SneakyThrows
  @Override
  public ModelConfiguration convert(ExportConfig source) {
    var config = new ModelConfiguration();
    config.setModule(MODULE_NAME);
    config.setConfigName(CONFIG_NAME);
    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(source));
    return config;
  }
}
