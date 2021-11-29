package org.folio.des.validator;

import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class BurSarFeesFinesExportParametersValidator implements Validator {
  @Override
  public boolean supports(Class<?> aClass) {
    return ExportTypeSpecificParameters.class.isAssignableFrom(aClass);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (target == null) {
      String msg = String.format("%s type should contain %s parameters", ExportType.BURSAR_FEES_FINES.getValue(),
        ExportTypeSpecificParameters.class.getSimpleName());
        errors.rejectValue(ExportTypeSpecificParameters.class.getSimpleName(), msg);
        throw new IllegalArgumentException(msg);
    }
    ExportTypeSpecificParameters specificParameters = (ExportTypeSpecificParameters) target;
    if (specificParameters.getBursarFeeFines() == null) {
      String msg = String.format("%s type should contain %s parameters", ExportType.BURSAR_FEES_FINES.getValue(),
                                  BursarFeeFines.class.getSimpleName());
      throw new IllegalArgumentException(msg);
    }
  }
}
