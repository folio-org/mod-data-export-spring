package org.folio.des.validator;

import org.folio.des.domain.dto.BursarExportJob;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Service
public class BursarFeesFinesExportParametersValidator implements Validator {
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
                                  BursarExportJob.class.getSimpleName());
      throw new IllegalArgumentException(msg);
    }
  }
}
