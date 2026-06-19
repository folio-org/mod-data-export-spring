package org.folio.des.validator.acquisition;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@AllArgsConstructor
@Log4j2
@Service
public class EdifactOrdersExportParametersValidator extends AbstractExportParametersValidator {
  private final EdifactOrdersScheduledParamsValidator edifactOrdersScheduledParamsValidator;

  @Override
  protected IntegrationTypeEnum getExpectedIntegrationType() {
    return IntegrationTypeEnum.ORDERING;
  }

  @Override
  protected void validateSpecific(VendorEdiOrdersExportConfig exportConfig, Errors errors) {
    EdiSchedule ediSchedule = exportConfig.getEdiSchedule();
    if (ediSchedule != null && ediSchedule.getScheduleParameters() != null) {
      edifactOrdersScheduledParamsValidator.validate(ediSchedule.getScheduleParameters(), errors);
    }
  }
}
