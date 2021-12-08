package org.folio.des.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.Errors;

@SpringBootTest(classes = { BurSarFeesFinesExportParametersValidator.class})
class BurSarFeesFinesExportParametersValidatorTest {
  @Autowired
  private BurSarFeesFinesExportParametersValidator validator;

  @Test
  @DisplayName("Should throw exception if specific parameters is Null")
  void shouldThrowExceptionIfSpecificParametersIsNull() {
    Errors errors = mock(Errors.class);
    assertThrows(IllegalArgumentException.class, () ->  validator.validate(null, errors));
  }

  @Test
  @DisplayName("Should throw exception if bursar fines fines is Null")
  void shouldThrowExceptionIfBursarFeeFinesIsNull() {
    Errors errors = mock(Errors.class);
    ExportTypeSpecificParameters specificParameters = new ExportTypeSpecificParameters();
    assertThrows(IllegalArgumentException.class, () ->  validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should pass validation if bursar fines fines is Null")
  void shouldPassValidationIfBursarFeeFinesIsNull() {
    Errors errors = mock(Errors.class);
    ExportTypeSpecificParameters specificParameters = new ExportTypeSpecificParameters();
    specificParameters.setBursarFeeFines(new BursarFeeFines());
    validator.validate(specificParameters, errors);
  }
}
