package org.folio.des.scheduling.quartz.converter.acquisition;

import java.util.Collections;
import java.util.Optional;

import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverter;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ExportConfigToEdifactTriggerConverter implements Converter<ExportConfig, ExportTrigger> {
  private final EdifactOrdersExportParametersValidator validator;
  private final ScheduleParametersToTriggerConverter scheduleParametersToTriggerConverter;

  @Override
  public ExportTrigger convert(ExportConfig exportConfig) {
    log.debug("ExportConfigToEdifactTriggerConverter:: converts from {}",  exportConfig);
    ExportTypeSpecificParameters specificParameters = exportConfig.getExportTypeSpecificParameters();
    Errors errors = new BeanPropertyBindingResult(specificParameters, "specificParameters");

    validator.validate(specificParameters, errors);

    if (isDisabledSchedule(exportConfig)) {
      return new ExportTrigger(true, Collections.emptySet());
    } else {
      ScheduleParameters scheduleParameters = Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
        .map(VendorEdiOrdersExportConfig::getEdiSchedule)
        .map(EdiSchedule::getScheduleParameters).orElse(null);
      return new ExportTrigger(false, scheduleParametersToTriggerConverter
        .convert(scheduleParameters, getTriggerGroup(exportConfig)));
    }
  }

  private boolean isDisabledSchedule(ExportConfig exportConfig) {
    Optional<EdiSchedule> ediSchedule = Optional.ofNullable(exportConfig.getExportTypeSpecificParameters())
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .map(VendorEdiOrdersExportConfig::getEdiSchedule);

    boolean isExportEnabled = ediSchedule.map(EdiSchedule::getEnableScheduledExport).orElse(false);
    return !isExportEnabled || ediSchedule
      .map(EdiSchedule::getScheduleParameters)
      .map(ScheduleParameters::getSchedulePeriod)
      .map(ScheduleParameters.SchedulePeriodEnum.NONE::equals)
      .orElse(false);
  }

  private String getTriggerGroup(ExportConfig exportConfig) {
    return exportConfig.getTenant() + "_" + QuartzConstants.EDIFACT_ORDERS_EXPORT_GROUP_NAME;
  }
}
