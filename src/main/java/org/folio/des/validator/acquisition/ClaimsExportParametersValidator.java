package org.folio.des.validator.acquisition;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.folio.des.domain.dto.VendorEdiOrdersExportConfig.FileFormatEnum.EDI;
import static org.folio.des.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING;
import static org.folio.des.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

@Log4j2
@Service
@AllArgsConstructor
public class ClaimsExportParametersValidator implements Validator {

  @Override
  public boolean supports(Class<?> classType) {
    return ExportTypeSpecificParameters.class.isAssignableFrom(classType);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (target == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing or invalid export specific parameters");
    }

    var exportConfig = ((ExportTypeSpecificParameters) target).getVendorEdiOrdersExportConfig();
    validateExportConfig(exportConfig);
    validateIntegrationType(exportConfig);
    validateFileFormat(exportConfig);
  }

  private void validateExportConfig(VendorEdiOrdersExportConfig exportConfig) {
    if (exportConfig == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing an export config");
    }
  }

  private void validateIntegrationType(VendorEdiOrdersExportConfig exportConfig) {
    var integrationType = exportConfig.getIntegrationType();
    if (integrationType == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing an integration type");
    }

    if (integrationType != CLAIMING) {
      throw new IllegalArgumentException(String.format("Export configuration is incomplete, an integration type is not set to %s", CLAIMING));
    }
  }

  private void validateFileFormat(VendorEdiOrdersExportConfig exportConfig) {
    var fileFormat = exportConfig.getFileFormat();
    if (fileFormat == null) {
      throw new IllegalArgumentException("Export configuration is incomplete, missing a file format");
    }

    if (fileFormat == EDI) {
      validateEdiConfig(exportConfig);
    }

    validateTransmissionType(exportConfig);
  }

  private void validateEdiConfig(VendorEdiOrdersExportConfig exportConfig) {
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

  private void validateTransmissionType(VendorEdiOrdersExportConfig exportConfig) {
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
