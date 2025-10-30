package org.folio.des.mapper.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.CopilotGenerated;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.validator.acquisition.ClaimsExportParametersValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CopilotGenerated(model = "Claude Sonnet 4.5")
@SpringBootTest(classes = {JacksonConfiguration.class, ClaimsExportConfigMapperImpl.class, ClaimsExportParametersValidator.class})
class ClaimsExportConfigMapperTest {

  @MockitoBean
  private ClaimsExportParametersValidator validator;
  @Autowired
  private ClaimsExportConfigMapper mapper;

  @Test
  @DisplayName("Should map ExportConfig DTO to Entity with vendor-specific config name")
  void testToEntity() {
    // Given
    String exportId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();

    ExportConfig dto = new ExportConfig();
    dto.setId(exportId);
    dto.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    dto.setTenant("test-tenant");

    VendorEdiOrdersExportConfig vendorConfig = new VendorEdiOrdersExportConfig();
    vendorConfig.setVendorId(vendorId);
    vendorConfig.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    vendorConfig.setFileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.EDI);

    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    parameters.setVendorEdiOrdersExportConfig(vendorConfig);
    dto.setExportTypeSpecificParameters(parameters);

    doNothing().when(validator).validate(any(), any());

    // When
    ExportConfigEntity entity = mapper.toEntity(dto);

    // Then
    assertNotNull(entity);
    assertEquals(UUID.fromString(exportId), entity.getId());
    assertEquals(ExportType.EDIFACT_ORDERS_EXPORT, entity.getType());
    assertEquals("test-tenant", entity.getTenant());

    // Verify config name format: TYPE_VENDORID_EXPORTID
    String expectedConfigName = ExportType.EDIFACT_ORDERS_EXPORT.getValue() + "_" + vendorId + "_" + exportId;
    assertEquals(expectedConfigName, entity.getConfigName());

    verify(validator).validate(any(), any());
  }

  @Test
  @DisplayName("Should map Entity to ExportConfig DTO")
  void testToDto() {
    // Given
    UUID id = UUID.randomUUID();
    ExportConfigEntity entity = new ExportConfigEntity();
    entity.setId(id);
    entity.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    entity.setTenant("test-tenant");
    entity.setConfigName("test-config");

    ExportTypeSpecificParameters specificParams = new ExportTypeSpecificParameters();
    entity.setExportTypeSpecificParameters(specificParams);

    // When
    ExportConfig dto = mapper.toDto(entity);

    // Then
    assertNotNull(dto);
    assertEquals(id.toString(), dto.getId());
    assertEquals(ExportType.EDIFACT_ORDERS_EXPORT, dto.getType());
    assertEquals("test-tenant", dto.getTenant());
    assertNotNull(dto.getExportTypeSpecificParameters());
  }

  @Test
  @DisplayName("Should validate export config before mapping")
  void testValidationIsCalled() {
    // Given
    String exportId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();

    ExportConfig dto = new ExportConfig();
    dto.setId(exportId);
    dto.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    dto.setTenant("test-tenant");

    VendorEdiOrdersExportConfig vendorConfig = new VendorEdiOrdersExportConfig();
    vendorConfig.setVendorId(vendorId);
    vendorConfig.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);

    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    parameters.setVendorEdiOrdersExportConfig(vendorConfig);
    dto.setExportTypeSpecificParameters(parameters);

    doNothing().when(validator).validate(any(), any());

    // When
    mapper.toEntity(dto);

    // Then - validator should be called with the parameters
    verify(validator, times(1)).validate(any(), any());
  }

  @Test
  @DisplayName("Should throw exception when validation fails")
  void testValidationFailure() {
    // Given
    String exportId = UUID.randomUUID().toString();

    ExportConfig dto = new ExportConfig();
    dto.setId(exportId);
    dto.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    dto.setTenant("test-tenant");

    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    parameters.setVendorEdiOrdersExportConfig(null); // Invalid - will cause validation error
    dto.setExportTypeSpecificParameters(parameters);

    doThrow(new IllegalArgumentException("Export configuration is incomplete"))
      .when(validator).validate(any(), any());

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(dto));
    verify(validator).validate(any(), any());
  }

}
