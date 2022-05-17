package org.folio.des.validator.acquisition;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Service
public class EdifactOrdersExportParametersValidator implements Validator {
  private EdifactOrdersScheduledParamsValidator edifactOrdersScheduledParamsValidator;

  @Override
  public boolean supports(Class<?> aClass) {
    return ExportTypeSpecificParameters.class.isAssignableFrom(aClass);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (target == null) {
      String msg = String.format("%s type should contain %s parameters", ExportType.EDIFACT_ORDERS_EXPORT.getValue(),
        ExportTypeSpecificParameters.class.getSimpleName());
        errors.rejectValue(ExportTypeSpecificParameters.class.getSimpleName(), msg);
        throw new IllegalArgumentException(msg);
    }
    ExportTypeSpecificParameters specificParameters = (ExportTypeSpecificParameters) target;
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = specificParameters.getVendorEdiOrdersExportConfig();
    if (vendorEdiOrdersExportConfig == null) {
      String msg = String.format("%s type should contain %s parameters", ExportType.EDIFACT_ORDERS_EXPORT.getValue(),
                            VendorEdiOrdersExportConfig.class.getSimpleName());
      throw new IllegalArgumentException(msg);
    }
    EdiConfig ediConfig = vendorEdiOrdersExportConfig.getEdiConfig();
    if (ediConfig != null &&
      (StringUtils.isEmpty(ediConfig.getLibEdiCode()) || ediConfig.getLibEdiType() == null || StringUtils.isEmpty(ediConfig.getVendorEdiCode()) || ediConfig.getVendorEdiType() == null)) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing library EDI code/Vendor EDI code");
    }
    if (vendorEdiOrdersExportConfig.getEdiFtp() != null && vendorEdiOrdersExportConfig.getEdiFtp().getFtpPort() == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing FTP/SFTP Port");
    }
    EdiSchedule ediSchedule = vendorEdiOrdersExportConfig.getEdiSchedule();
    if (vendorEdiOrdersExportConfig.getEdiSchedule() != null &&
                  ediSchedule.getScheduleParameters() != null) {
      edifactOrdersScheduledParamsValidator.validate(ediSchedule.getScheduleParameters(), errors);
    }
  }
}
