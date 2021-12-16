package org.folio.des.converter.aqcuisition;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.UUID;

@AllArgsConstructor
@Log4j2
@Service
public final class EdifactExportConfigToModelConfigConverter implements Converter<ExportConfig, ModelConfiguration> {
  private static final String CONFIG_DESCRIPTION = "Edifact orders export configuration parameters";

  private final ObjectMapper objectMapper;
  private EdifactOrdersExportParametersValidator validator;

  @SneakyThrows
  @Override
  public ModelConfiguration convert(ExportConfig exportConfig) {
    Errors errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
    validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);

    var config = new ModelConfiguration();
    config.setId(exportConfig.getId());
    config.setModule(DEFAULT_MODULE_NAME);
    UUID vendorId = exportConfig.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getVendorId();
    config.setConfigName(exportConfig.getType().getValue() + "_" + vendorId.toString());

    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(exportConfig));
    return config;
  }
}
