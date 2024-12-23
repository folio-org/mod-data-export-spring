package org.folio.des.converter.aqcuisition;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;

@Log4j2
@Service
@AllArgsConstructor
public final class ClaimsExportConfigToModelConfigConverter implements Converter<ExportConfig, ModelConfiguration> {

  private static final String CONFIG_DESCRIPTION = "Claims export configuration parameters";

  private final ObjectMapper objectMapper;

  @Override
  @SneakyThrows
  public ModelConfiguration convert(ExportConfig exportConfig) {
    var config = new ModelConfiguration();
    config.setId(exportConfig.getId());
    config.setModule(DEFAULT_MODULE_NAME);

    var ediOrdersExportConfig = exportConfig.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig();
    config.setConfigName(exportConfig.getType().getValue() + "_" + ediOrdersExportConfig.getVendorId().toString() + "_" + exportConfig.getId());
    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(exportConfig));

    return config;
  }
}
