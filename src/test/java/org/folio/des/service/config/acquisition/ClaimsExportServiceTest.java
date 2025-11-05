package org.folio.des.service.config.acquisition;

import static org.folio.des.support.TestUtils.setInternalState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.mapper.acquisition.ClaimsExportConfigMapperImpl;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.des.validator.acquisition.ClaimsExportParametersValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimsExportServiceTest {

  private static final UUID CONFIG_ID = UUID.randomUUID();
  private static final UUID VENDOR_ID = UUID.randomUUID();
  private static final ExportConfig CLAIMS_EXPORT_CONFIG = new ExportConfig()
    .id(CONFIG_ID.toString())
    .type(ExportType.CLAIMS)
    .configName("%s_%s_%s".formatted(ExportType.CLAIMS, VENDOR_ID, CONFIG_ID))
    .exportTypeSpecificParameters(new ExportTypeSpecificParameters()
      .vendorEdiOrdersExportConfig(new VendorEdiOrdersExportConfig()
        .configName("name")
        .vendorId(VENDOR_ID)
        .integrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING)
        .transmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD)
        .fileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.CSV)));

  private ExportConfigRepository repository;
  private ClaimsExportService service;

  @BeforeEach
  void setUp() {
    var validatorKey = "%s-%s".formatted(ExportType.CLAIMS, ExportTypeSpecificParameters.class.getName());
    var validator = new ClaimsExportParametersValidator();
    var exportConfigValidatorResolver = new ExportConfigValidatorResolver(Map.of(validatorKey, validator));

    var defaultExportConfigMapper = new DefaultExportConfigMapper();
    var claimsExportConfigMapper = new ClaimsExportConfigMapperImpl();
    var exportConfigMapperResolver = new ExportConfigMapperResolver(Map.of(ExportType.CLAIMS, claimsExportConfigMapper), defaultExportConfigMapper);
    setInternalState(claimsExportConfigMapper, "objectMapper", new JacksonConfiguration().entityObjectMapper());
    setInternalState(claimsExportConfigMapper, "validator", validator);

    repository = Mockito.mock(ExportConfigRepository.class);
    service = new ClaimsExportService(repository, claimsExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver);
  }

  @Test
  @DisplayName("Set new configuration")
  void testPostConfig() {
    when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    var response = service.postConfig(CLAIMS_EXPORT_CONFIG);

    assertEquals(CLAIMS_EXPORT_CONFIG, response);
  }

  @Test
  @DisplayName("Update configuration")
  void testUpdateConfig() {
    when(repository.findById(UUID.fromString(CLAIMS_EXPORT_CONFIG.getId())))
      .thenReturn(java.util.Optional.of(new ExportConfigEntity()));

    service.updateConfig(CLAIMS_EXPORT_CONFIG.getId(), CLAIMS_EXPORT_CONFIG);

    verify(repository).save(any());
  }

}
