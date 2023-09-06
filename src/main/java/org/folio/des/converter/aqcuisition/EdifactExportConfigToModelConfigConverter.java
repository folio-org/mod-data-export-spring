package org.folio.des.converter.aqcuisition;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;

import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Log4j2
@Service
public final class EdifactExportConfigToModelConfigConverter implements Converter<ExportConfig, ModelConfiguration> {
  private static final String CONFIG_DESCRIPTION = "Edifact orders export configuration parameters";
  private static final String INCORRECT_UUID_FOR_SCHEDULED_PARAMETER_MSG = "Incorrect UUID for scheduled parameter provided and will be replaced with : {}";

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
    VendorEdiOrdersExportConfig ediOrdersExportConfig = exportConfig.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig();
    UUID vendorId = ediOrdersExportConfig.getVendorId();
    config.setConfigName(exportConfig.getType().getValue() + "_" + vendorId.toString() + "_" + exportConfig.getId());
    Optional.ofNullable(ediOrdersExportConfig.getEdiSchedule())
      .map(EdiSchedule::getScheduleParameters)
      .filter(scheduleParameters -> isScheduledParameterIdNotValid(exportConfig, scheduleParameters))
      .ifPresent(scheduleParameters -> {
        log.warn(INCORRECT_UUID_FOR_SCHEDULED_PARAMETER_MSG, exportConfig.getId());
        scheduleParameters.setId(UUID.fromString(exportConfig.getId()));
      });
    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(exportConfig));
    log.debug("EdifactExportConfigToModelConfigConverter:: Convert process result from {} into {}", exportConfig, config);
    return config;
  }

  private boolean isScheduledParameterIdNotValid(ExportConfig exportConfig, ScheduleParameters scheduleParameters) {
    return Objects.isNull(scheduleParameters.getId()) ||
              !UUID.fromString(exportConfig.getId()).equals(scheduleParameters.getId());
  }
}
