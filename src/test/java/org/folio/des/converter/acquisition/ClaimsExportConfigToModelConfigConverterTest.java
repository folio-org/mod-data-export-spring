package org.folio.des.converter.acquisition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.converter.aqcuisition.ClaimsExportConfigToModelConfigConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = { JacksonConfiguration.class, ClaimsExportConfigToModelConfigConverter.class })
class ClaimsExportConfigToModelConfigConverterTest {

  @Autowired
  private ClaimsExportConfigToModelConfigConverter converter;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testConverterIfExportConfigIsValid() throws JsonProcessingException {
    var expId = UUID.randomUUID().toString();
    var vendorId = UUID.randomUUID();

    var ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.CLAIMS);

    var vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    var parameters = new ExportTypeSpecificParameters();
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    var actConfig = converter.convert(ediConfig);
    var actExportConfig = objectMapper.readValue(actConfig.getValue(), ExportConfig.class);
    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(ExportType.CLAIMS + "_" + vendorId + "_" + expId, actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled()),
      () -> assertEquals(expId, actExportConfig.getId())
    );
  }

  @ParameterizedTest
  @CsvSource({ "CLAIMS, CLAIMS" })
  void testConverterIfExportConfigIsValidNoDuplicateError(ExportType exportType) {
    var expId = UUID.randomUUID().toString();
    var vendorId = UUID.randomUUID();

    var ediConfig = new ExportConfig();
    ediConfig.setType(exportType);
    ediConfig.setId(expId);

    var vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    var parameters = new ExportTypeSpecificParameters();
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    var actConfig = converter.convert(ediConfig);
    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(ExportType.CLAIMS + "_" + vendorId + "_" + expId, actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled())
    );
  }
}
