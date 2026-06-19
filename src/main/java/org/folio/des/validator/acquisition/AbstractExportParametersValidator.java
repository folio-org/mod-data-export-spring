package org.folio.des.validator.acquisition;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.folio.des.domain.dto.VendorEdiOrdersExportConfig.FileFormatEnum.EDI;
import static org.folio.des.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

public abstract class AbstractExportParametersValidator implements Validator {

  @Override
  public boolean supports(Class<?> classType) {
    return ExportTypeSpecificParameters.class.isAssignableFrom(classType);
  }

  @Override
  public final void validate(Object target, Errors errors) {
    if (target == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing or invalid export specific parameters");
    }

    var exportConfig = ((ExportTypeSpecificParameters) target).getVendorEdiOrdersExportConfig();
    if (exportConfig == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing an export config");
    }

    var expectedIntegrationType = getExpectedIntegrationType();
    if (expectedIntegrationType != null) {
      if (exportConfig.getIntegrationType() == null) {
        throw new IllegalArgumentException("Export configuration is incomplete, missing an integration type");
      }

      if (exportConfig.getIntegrationType() != expectedIntegrationType) {
        throw new IllegalArgumentException(String.format("Export configuration is incomplete, an integration type is not set to %s", expectedIntegrationType));
      }
    }

    validateFileFormat(exportConfig);
    validateSpecific(exportConfig, errors);
  }

  protected abstract IntegrationTypeEnum getExpectedIntegrationType();

  protected void validateSpecific(VendorEdiOrdersExportConfig exportConfig, Errors errors) {
  }

  protected void validateFileFormat(VendorEdiOrdersExportConfig exportConfig) {
    var fileFormat = exportConfig.getFileFormat();
    if (fileFormat == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing a file format");
    }

    if (fileFormat == EDI) {
      validateEdiConfig(exportConfig);
    }

    validateTransmissionType(exportConfig);
  }

  protected void validateEdiConfig(VendorEdiOrdersExportConfig exportConfig) {
    var ediConfig = exportConfig.getEdiConfig();
    if (ediConfig != null) {
      if (CollectionUtils.isEmpty(ediConfig.getAccountNoList())) {
        throw new IllegalArgumentException("Export configuration is incomplete, missing Vendor Account Number(s)");
      }

      if (StringUtils.isEmpty(ediConfig.getLibEdiCode()) || StringUtils.isEmpty(ediConfig.getVendorEdiCode())) {
        throw new IllegalArgumentException("Export configuration is incomplete, missing library EDI code/Vendor EDI code");
      }
    }
  }

  protected void validateTransmissionType(VendorEdiOrdersExportConfig exportConfig) {
    var transmissionMethod = exportConfig.getTransmissionMethod();
    if (transmissionMethod == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing a transmission type");
    }

    if (transmissionMethod == FTP) {
      var ediFtp = exportConfig.getEdiFtp();
      if (ediFtp == null) {
        throw new IllegalArgumentException("Export configuration is incomplete, missing EDI FTP Properties");
      }

      if (ediFtp.getServerAddress() == null) {
        throw new IllegalArgumentException("Export configuration is incomplete, missing FTP/SFTP Server Address");
      }

      if (ediFtp.getFtpPort() == null) {
        throw new IllegalArgumentException("Export configuration is incomplete, missing FTP/SFTP Port");
      }
    }
  }
}
