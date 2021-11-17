package org.folio.des.validator;

import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Service
public class BurSarFeesFinesExportConfigValidator implements Validator {
  @Override
  public boolean supports(Class<?> aClass) {
    return ExportConfig.class.isAssignableFrom(aClass);
  }

  @Override
  public void validate(Object target, Errors errors) {
    ExportConfig exportConfig = (ExportConfig) target;
    ExportType type = exportConfig.getType();
    ExportTypeSpecificParameters exportTypeSpecificParameters = exportConfig.getExportTypeSpecificParameters();
    if (type == ExportType.BURSAR_FEES_FINES && exportTypeSpecificParameters.getBursarFeeFines() == null) {
      throw new IllegalArgumentException(
        String.format("%s type should contain %s parameters", type, BursarFeeFines.class.getSimpleName()));
    }
  }
}
