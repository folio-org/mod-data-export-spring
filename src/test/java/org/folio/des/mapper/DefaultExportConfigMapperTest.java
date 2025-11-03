package org.folio.des.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.CopilotGenerated;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

@CopilotGenerated(model = "Claude Sonnet 4.5")
@SpringBootTest(classes = {JacksonConfiguration.class, DefaultExportConfigMapper.class})
class DefaultExportConfigMapperTest {

  @Autowired
  private DefaultExportConfigMapper mapper;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should map ExportConfig DTO to Entity")
  void testToEntity() {
    // Given
    String id = UUID.randomUUID().toString();
    ExportConfig dto = new ExportConfig();
    dto.setId(id);
    dto.setType(ExportType.BURSAR_FEES_FINES);
    dto.setTenant("test-tenant");
    dto.setScheduleFrequency(7);
    dto.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);
    dto.setScheduleTime("10:00:00");

    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    dto.setExportTypeSpecificParameters(parameters);

    // When
    ExportConfigEntity entity = mapper.toEntity(dto);

    // Then
    assertNotNull(entity);
    assertEquals(UUID.fromString(id), entity.getId());
    assertEquals(ExportType.BURSAR_FEES_FINES.getValue(), entity.getType());
    assertEquals("test-tenant", entity.getTenant());
    assertEquals(7, entity.getScheduleFrequency());
    assertEquals("WEEK", entity.getSchedulePeriod());
    assertEquals("10:00:00", entity.getScheduleTime());
    assertEquals("export_config_parameters", entity.getConfigName()); // Default config name for BURSAR
  }

  @Test
  @DisplayName("Should map Entity to ExportConfig DTO")
  void testToDto() {
    // Given
    UUID id = UUID.randomUUID();
    ExportConfigEntity entity = new ExportConfigEntity();
    entity.setId(id);
    entity.setType(ExportType.BURSAR_FEES_FINES.getValue());
    entity.setTenant("test-tenant");
    entity.setConfigName("test-config");
    entity.setScheduleFrequency(7);
    entity.setSchedulePeriod("WEEK");
    entity.setScheduleTime("10:00:00");

    ExportTypeSpecificParameters specificParams = new ExportTypeSpecificParameters();
    entity.setExportTypeSpecificParameters(specificParams);

    // When
    ExportConfig dto = mapper.toDto(entity);

    // Then
    assertNotNull(dto);
    assertEquals(id.toString(), dto.getId());
    assertEquals(ExportType.BURSAR_FEES_FINES, dto.getType());
    assertEquals("test-tenant", dto.getTenant());
    assertEquals(7, dto.getScheduleFrequency());
    assertEquals(ExportConfig.SchedulePeriodEnum.WEEK, dto.getSchedulePeriod());
    assertEquals("10:00:00", dto.getScheduleTime());
    assertNotNull(dto.getExportTypeSpecificParameters());
  }

  @Test
  @DisplayName("Should use default config name for BURSAR_FEES_FINES type")
  void testGetConfigNameForBursarType() {
    // Given
    ExportConfig dto = new ExportConfig();
    dto.setId(UUID.randomUUID().toString());
    dto.setType(ExportType.BURSAR_FEES_FINES);
    dto.setTenant("test-tenant");
    dto.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());

    // When
    ExportConfigEntity entity = mapper.toEntity(dto);

    // Then
    assertEquals("export_config_parameters", entity.getConfigName());
  }

  @Test
  @DisplayName("Should use type value as config name for non-BURSAR types")
  void testGetConfigNameForOtherTypes() {
    // Given
    ExportConfig dto = new ExportConfig();
    dto.setId(UUID.randomUUID().toString());
    dto.setType(ExportType.CIRCULATION_LOG);
    dto.setTenant("test-tenant");
    dto.setExportTypeSpecificParameters(new ExportTypeSpecificParameters());

    // When
    ExportConfigEntity entity = mapper.toEntity(dto);

    // Then
    assertEquals(ExportType.CIRCULATION_LOG.getValue(), entity.getConfigName());
  }

}

