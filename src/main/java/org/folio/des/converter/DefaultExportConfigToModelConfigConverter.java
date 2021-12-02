package org.folio.des.converter;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
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
public final class DefaultExportConfigToModelConfigConverter implements Converter<ExportConfig, ModelConfiguration> {
  private static final String CONFIG_DESCRIPTION = "Data export configuration parameters";

  private final ObjectMapper objectMapper;

  @SneakyThrows
  @Override
  public ModelConfiguration convert(ExportConfig source) {
    var config = new ModelConfiguration();
    config.setId(source.getId());
    config.setModule(DEFAULT_MODULE_NAME);
    if (source.getType() == null || ExportType.BURSAR_FEES_FINES.equals(source.getType())) {
      config.setConfigName(DEFAULT_CONFIG_NAME);
    } else {
      config.setConfigName(source.getType().getValue());
    }
    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(source));
    return config;
  }
}
