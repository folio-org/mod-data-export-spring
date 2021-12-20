package org.folio.des.converter.acquisition;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.folio.des.config.JacksonConfiguration;
import org.folio.des.converter.aqcuisition.EdifactExportConfigToModelConfigConverter;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { JacksonConfiguration.class, EdifactExportConfigToModelConfigConverter.class,
                            EdifactOrdersExportParametersValidator.class})
class EdifactExportConfigToModelConfigConverterTest {
  @Autowired
  EdifactExportConfigToModelConfigConverter converter;

  @Test
  void testConverterIfExportConfigIsValid() {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    ModelConfiguration actConfig = converter.convert(ediConfig);

    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(ExportType.EDIFACT_ORDERS_EXPORT + "_" + vendorId.toString(), actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled())
    );
  }

  @ParameterizedTest
  @CsvSource({
    "EDIFACT_ORDERS_EXPORT, EDIFACT_ORDERS_EXPORT"
  })
  void testConverterIfExportConfigIsValidAndTypeIsProvided(ExportType exportType) {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setType(exportType);
    ediConfig.setId(expId);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();

    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    ediConfig.exportTypeSpecificParameters(parameters);

    ModelConfiguration actConfig = converter.convert(ediConfig);

    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(ExportType.EDIFACT_ORDERS_EXPORT + "_" + vendorId.toString(), actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled())
    );
  }

  @ParameterizedTest
  @CsvSource({
    "EDIFACT_ORDERS_EXPORT, EDIFACT_ORDERS_EXPORT"
  })
  void shouldThrowExceptionIfExportConfigIsNotValidAndTypeIsProvided(ExportType exportType, String expConfigName) {
    String expId = UUID.randomUUID().toString();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setType(exportType);
    ediConfig.setId(expId);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();

    BursarFeeFines bursarFeeFines = new BursarFeeFines();
    bursarFeeFines.setDaysOutstanding(9);
    bursarFeeFines.setPatronGroups(List.of(UUID.randomUUID().toString()));
    parameters.setBursarFeeFines(bursarFeeFines);

    ediConfig.exportTypeSpecificParameters(parameters);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                  () -> converter.convert(ediConfig));
  }
}
