package org.folio.des.validator.acquisition;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.Errors;

@SpringBootTest(classes = { EdifactOrdersExportParametersValidator.class, EdifactOrdersScheduledParamsValidator.class})
class EdifactOrdersExportParametersValidatorTest {
  @Autowired
  private EdifactOrdersExportParametersValidator validator;

  @Test
  @DisplayName("Should throw exception if specific parameters is Null")
  void shouldThrowExceptionIfSpecificParametersIsNull() {
    Errors errors = mock(Errors.class);
    assertThrows(IllegalArgumentException.class, () ->  validator.validate(null, errors));
  }

  @Test
  @DisplayName("Should throw exception if edifact config is Null")
  void shouldThrowExceptionIfBursarFeeFinesIsNull() {
    Errors errors = mock(Errors.class);
    ExportTypeSpecificParameters specificParameters = new ExportTypeSpecificParameters();
    assertThrows(IllegalArgumentException.class, () ->  validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should pass validation if edifact is not Null")
  void shouldPassValidationIfBursarFeeFinesIsNotNull() {
    Errors errors = mock(Errors.class);
    ExportTypeSpecificParameters specificParameters = new ExportTypeSpecificParameters();
    specificParameters.setVendorEdiOrdersExportConfig(new VendorEdiOrdersExportConfig());
    validator.validate(specificParameters, errors);
  }
}
