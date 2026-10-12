package org.folio.des.validator.acquisition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.folio.des.domain.dto.EdiConfig;
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
  void shouldThrowExceptionIfEdifactConfigIsNull() {
    Errors errors = mock(Errors.class);
    ExportTypeSpecificParameters specificParameters = new ExportTypeSpecificParameters();
    assertThrows(IllegalArgumentException.class, () ->  validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should pass validation if edifact is not Null")
  void shouldPassValidationIfEdifactConfigIsNotNull() {
    Errors errors = mock(Errors.class);
    ExportTypeSpecificParameters specificParameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.CSV);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    specificParameters.setVendorEdiOrdersExportConfig(config);
    validator.validate(specificParameters, errors);
  }

  @Test
  @DisplayName("Should pass validation if default edi config has no account numbers")
  void shouldPassValidationIfDefaultEdiConfigHasNoAccountNumbers() {
    Errors errors = mock(Errors.class);
    var specificParameters = getEdiParameters(true);
    assertDoesNotThrow(() -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if non-default edi config has no account numbers")
  void shouldThrowExceptionIfNonDefaultEdiConfigHasNoAccountNumbers() {
    Errors errors = mock(Errors.class);
    var specificParameters = getEdiParameters(false);
    var exception = assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
    assertEquals("Export configuration is incomplete, missing Vendor Account Number(s)", exception.getMessage());
  }

  private ExportTypeSpecificParameters getEdiParameters(boolean isDefaultConfig) {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setIsDefaultConfig(isDefaultConfig);
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN));
    return new ExportTypeSpecificParameters().vendorEdiOrdersExportConfig(config);
  }
}
