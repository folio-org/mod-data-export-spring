package org.folio.des.validator;

import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Service
public class BurSarFeesFinesExportSpecificParametersValidator implements Validator {
  @Override
  public boolean supports(Class<?> aClass) {
    return ExportTypeSpecificParameters.class.isAssignableFrom(aClass);
  }

  @Override
  public void validate(Object target, Errors errors) {
    ExportTypeSpecificParameters specificParameters = (ExportTypeSpecificParameters) target;
    if (specificParameters.getBursarFeeFines() == null) {
      throw new IllegalArgumentException(
        String.format("%s type should contain %s parameters", ExportType.BURSAR_FEES_FINES.getValue(),
                                BursarFeeFines.class.getSimpleName()));
    }
  }
}
