package org.folio.des.converter;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { JacksonConfiguration.class, DefaultExportConfigToModelConfigConverter.class})
public class DefaultExportConfigToModelConfigConverterTest {
  @Autowired
  DefaultExportConfigToModelConfigConverter converter;

  @Test
  void testConverterIfExportConfigIsValid() {
    String expId = UUID.randomUUID().toString();
    ExportConfig bursarExportConfig = new ExportConfig();
    bursarExportConfig.setId(expId);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    BursarFeeFines bursarFeeFines = new BursarFeeFines();
    bursarFeeFines.setDaysOutstanding(9);
    bursarFeeFines.setPatronGroups(List.of(UUID.randomUUID().toString()));
    parameters.setBursarFeeFines(bursarFeeFines);
    bursarExportConfig.exportTypeSpecificParameters(parameters);

    ModelConfiguration actConfig = converter.convert(bursarExportConfig);

    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(DEFAULT_CONFIG_NAME, actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled())
    );
  }
}
