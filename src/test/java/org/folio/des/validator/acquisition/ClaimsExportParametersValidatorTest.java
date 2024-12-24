package org.folio.des.validator.acquisition;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.folio.des.CopilotGenerated;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.Errors;

import java.util.List;

@CopilotGenerated(partiallyGenerated = true)
class ClaimsExportParametersValidatorTest {

  private final ClaimsExportParametersValidator validator = new ClaimsExportParametersValidator();

  // Other

  @Test
  @DisplayName("Should throw exception if specific parameters is null")
  void shouldThrowExceptionIfSpecificParametersIsNull() {
    var errors = mock(Errors.class);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(null, errors));
  }

  @Test
  @DisplayName("Should throw exception if export config is null")
  void shouldThrowExceptionIfExportConfigIsNull() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if integration type is null")
  void shouldThrowExceptionIfIntegrationTypeIsNull() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    specificParameters.setVendorEdiOrdersExportConfig(new VendorEdiOrdersExportConfig());
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if integration type is not CLAIMING")
  void shouldThrowExceptionIfIntegrationTypeIsNotClaiming() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING);
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if file format is null")
  void shouldThrowExceptionIfFileFormatIsNull() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if EDI config is incomplete")
  void shouldThrowExceptionIfEdiConfigIsIncomplete() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  // Claiming, CSV, FTP

  @Test
  @DisplayName("Should pass validation if all required fields are set for CSV format Ftp")
  void shouldPassValidationIfAllRequiredFieldsAreSetForCsvFormatFtp() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.CSV);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP);
    config.setEdiFtp(new EdiFtp().serverAddress("serverAddress").ftpPort(1));
    specificParameters.setVendorEdiOrdersExportConfig(config);
    validator.validate(specificParameters, errors);
  }

  // Claiming, CSV, File Download

  @Test
  @DisplayName("Should pass validation if all required fields are set for CSV format File Download")
  void shouldPassValidationIfAllRequiredFieldsAreSetForCsvFormatFileDownload() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.CSV);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    specificParameters.setVendorEdiOrdersExportConfig(config);
    validator.validate(specificParameters, errors);
  }

  // Claiming, EDI, FTP

  @Test
  @DisplayName("Should pass validation if all required fields are set for transmission type Ftp")
  void shouldPassValidationIfAllRequiredFieldsAreSetTransmissionTypeFtp() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP);
    config.setEdiFtp(new EdiFtp().serverAddress("serverAddress").ftpPort(1));
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo"))
    );
    specificParameters.setVendorEdiOrdersExportConfig(config);
    validator.validate(specificParameters, errors);
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type Ftp missing Edi Ftp")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFtpMissingEdiFtp() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP);
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo"))
    );
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type Ftp missing Server Address")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFtpMissingServerAddress() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP);
    config.setEdiFtp(new EdiFtp().ftpPort(1));
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo"))
    );
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type Ftp missing Ftp Port")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFtpMissingFtpPort() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP);
    config.setEdiFtp(new EdiFtp().serverAddress("serverAddress"));
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo"))
    );
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  // Claiming, EDI, File Download

  @Test
  @DisplayName("Should pass validation if all required fields are set for transmission type File download")
  void shouldPassValidationIfAllRequiredFieldsAreSetTransmissionTypeFileDownload() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo")));
    specificParameters.setVendorEdiOrdersExportConfig(config);
    validator.validate(specificParameters, errors);
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type File download missing Account No")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFileDownloadMissingAccountNo() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN));
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type File download missing Lib Edi Code")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFileDownloadMissingLibEdiCode() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setEdiConfig(new EdiConfig()
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo")));
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type File download missing Lib Edi Type")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFileDownloadMissingLibEdiType() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .vendorEdiCode("vendorCode")
      .vendorEdiType(EdiConfig.VendorEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo")));
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type File download missing Vendor Edi Type")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFileDownloadMissingVendorEdiType() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .accountNoList(List.of("accountNo")));
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }

  @Test
  @DisplayName("Should throw exception if some required fields are set for transmission type File download missing Vendor Edi Code")
  void shouldThrowExceptionIfSomeRequiredFieldsAreSetTransmissionTypeFileDownloadMissingVendorEdiCode() {
    var errors = mock(Errors.class);
    var specificParameters = new ExportTypeSpecificParameters();
    var config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);
    config.setTransmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setEdiConfig(new EdiConfig()
      .libEdiCode("libCode")
      .libEdiType(EdiConfig.LibEdiTypeEnum._014_EAN)
      .vendorEdiCode("vendorCode")
      .accountNoList(List.of("accountNo")));
    specificParameters.setVendorEdiOrdersExportConfig(config);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(specificParameters, errors));
  }
}
