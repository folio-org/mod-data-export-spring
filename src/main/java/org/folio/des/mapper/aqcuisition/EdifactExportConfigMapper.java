package org.folio.des.mapper.aqcuisition;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.folio.des.mapper.BaseExportConfigMapper;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.service.config.ExportConfigConstants;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Mapper(imports = {ExportConfigConstants.class, ExportTypeSpecificParameters.class, ExportTypeSpecificParametersWithLegacyBursar.class})
public abstract class EdifactExportConfigMapper extends BaseExportConfigMapper {

  private static final String CONFIG_DESCRIPTION = "Edifact orders export configuration parameters";
  private static final String INCORRECT_UUID_FOR_SCHEDULED_PARAMETER_MSG = "Incorrect UUID for scheduled parameter provided and will be replaced with : {}";

  @Autowired
  private EdifactOrdersExportParametersValidator validator;

  protected String getConfigName(ExportConfig exportConfig) {
    Errors errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
    validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);

    VendorEdiOrdersExportConfig ediOrdersExportConfig = exportConfig.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig();
    UUID vendorId = ediOrdersExportConfig.getVendorId();
    Optional.ofNullable(ediOrdersExportConfig.getEdiSchedule())
      .map(EdiSchedule::getScheduleParameters)
      .filter(scheduleParameters -> isScheduledParameterIdNotValid(exportConfig, scheduleParameters))
      .ifPresent(scheduleParameters -> {
        log.warn(INCORRECT_UUID_FOR_SCHEDULED_PARAMETER_MSG, exportConfig.getId());
        scheduleParameters.setId(UUID.fromString(exportConfig.getId()));
      });
    return exportConfig.getType().getValue() + "_" + vendorId.toString() + "_" + exportConfig.getId();
  }

  private boolean isScheduledParameterIdNotValid(ExportConfig exportConfig, ScheduleParameters scheduleParameters) {
    return Objects.isNull(scheduleParameters.getId()) || !UUID.fromString(exportConfig.getId()).equals(scheduleParameters.getId());
  }

  protected String getConfigDescription() {
    return CONFIG_DESCRIPTION;
  }

}
