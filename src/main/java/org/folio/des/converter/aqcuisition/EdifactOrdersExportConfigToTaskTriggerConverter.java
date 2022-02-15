package org.folio.des.converter.aqcuisition;

import static org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum.NONE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.acquisition.AcqBaseExportTaskTrigger;
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

    List<ExportTaskTrigger> exportTaskTriggers = new ArrayList<>(1);
    Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
            .map(VendorEdiOrdersExportConfig::getEdiSchedule)
            .ifPresent(ediSchedule -> {
              ScheduleParameters scheduleParameters = ediSchedule.getScheduleParameters();
              if (scheduleParameters != null && !NONE.equals(scheduleParameters.getSchedulePeriod())) {
                if (scheduleParameters.getId() == null) {
                  scheduleParameters.setId(UUID.fromString(exportConfig.getId()));
                }
                scheduleParameters.setTimeZone(scheduleParameters.getTimeZone());
                var trigger = new AcqBaseExportTaskTrigger(scheduleParameters, ediSchedule.getEnableScheduledExport());
                exportTaskTriggers.add(trigger);
              }
            });
    return exportTaskTriggers;
  }
}
