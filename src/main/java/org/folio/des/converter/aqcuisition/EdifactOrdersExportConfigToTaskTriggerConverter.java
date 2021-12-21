package org.folio.des.converter.aqcuisition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.folio.des.domain.dto.Edi;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.acquisition.EdifactOrdersExportTaskTrigger;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Service
public class EdifactOrdersExportConfigToTaskTriggerConverter implements Converter<ExportConfig, List<ExportTaskTrigger>>  {
  private EdifactOrdersExportParametersValidator validator;

  @Override
  public List<ExportTaskTrigger> convert(ExportConfig exportConfig) {
    ExportTypeSpecificParameters specificParameters = exportConfig.getExportTypeSpecificParameters();
    Errors errors = new BeanPropertyBindingResult(specificParameters, "specificParameters");
    validator.validate(specificParameters, errors);
    if (ExportConfig.SchedulePeriodEnum.NONE != exportConfig.getSchedulePeriod()) {
      List<ExportTaskTrigger> exportTaskTriggers = new ArrayList<>();
      Optional<ScheduleParameters> defScheduleParameters = Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
                              .map(VendorEdiOrdersExportConfig::getDefaultEdiConfig)
                              .map(EdiConfig::getEdi)
                              .map(Edi::getEdiSchedule)
                              .filter(EdiSchedule::getEnableScheduledExport)
                              .map(EdiSchedule::getScheduleParameters);

      List<EdiConfig> accountEdiConfigs = Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
                .map(VendorEdiOrdersExportConfig::getEdiConfigs)
                .orElse(Collections.emptyList());
      accountEdiConfigs.forEach(ediConfig -> {
                Optional.ofNullable(ediConfig)
                        .map(EdiConfig::getEdi)
                        .map(Edi::getEdiSchedule)
                        .filter(EdiSchedule::getEnableScheduledExport)
                        .map(EdiSchedule::getScheduleParameters)
                        .ifPresent(accEdiConfig -> exportTaskTriggers.add(new EdifactOrdersExportTaskTrigger(accEdiConfig)));
      });
      return exportTaskTriggers;
    }
    return Collections.emptyList();
  }
}
