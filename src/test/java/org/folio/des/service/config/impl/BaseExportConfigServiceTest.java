package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;
import static org.folio.des.support.TestUtils.getBursarExportConfig;
import static org.folio.des.support.TestUtils.setInternalState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.validator.BursarFeesFinesExportParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BaseExportConfigServiceTest {

  private static final ExportConfigEntity CONFIG_ENTITY = new ExportConfigEntity()
    .setId(UUID.fromString("6c163f99-9df5-419d-9174-da638a1c76ed"))
    .setSchedulePeriod("DAY")
    .setWeekDays(List.of("FRIDAY", "MONDAY"));

  private ExportConfigRepository repository;
  private BaseExportConfigService service;

  @BeforeEach
  void setUp() {
    var validatorKey = "%s-%s".formatted(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class.getName());
    var exportConfigValidatorResolver = new ExportConfigValidatorResolver(Map.of(validatorKey, new BursarFeesFinesExportParametersValidator()));

    var defaultExportConfigMapper = new DefaultExportConfigMapper();
    var exportConfigMapperResolver = new ExportConfigMapperResolver(Map.of(), defaultExportConfigMapper);
    setInternalState(defaultExportConfigMapper, "objectMapper", new JacksonConfiguration().entityObjectMapper());

    repository = Mockito.mock(ExportConfigRepository.class);
    service = new BaseExportConfigService(repository, defaultExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver);
  }

  @Test
  @DisplayName("Set new configuration")
  void testPostConfig() {
    var exportConfig = getBursarExportConfig();
    when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    var response = service.postConfig(exportConfig);

    assertEquals(exportConfig, response);
  }

  @Test
  @DisplayName("Should not create new configuration without specific parameters")
  void testPostConfigWithoutSpecificParameters() {
    var bursarExportConfig = new ExportConfig();

    assertThrows(IllegalStateException.class, () -> service.postConfig(bursarExportConfig));

    verify(repository, times(0)).save(any());
  }

  @Test
  @DisplayName("Should not create new configuration without bursar parameters")
  void testPostConfigWithoutBursarParameters() {
    var bursarExportConfig = new ExportConfig().exportTypeSpecificParameters(new ExportTypeSpecificParameters());

    assertThrows(IllegalArgumentException.class, () -> service.postConfig(bursarExportConfig));

    verify(repository, times(0)).save(any());
  }

  @Test
  @DisplayName("Update configuration")
  void testUpdateConfig() {
    var exportConfig = getBursarExportConfig();

    when(repository.findById(UUID.fromString(exportConfig.getId())))
      .thenReturn(java.util.Optional.of(new ExportConfigEntity()));

    service.updateConfig(exportConfig.getId(), exportConfig);

    verify(repository).save(any());
  }

  @Test
  @DisplayName("Config exists and parsed correctly")
  void testGetFirstConfig() {
    when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(CONFIG_ENTITY)));

    var config = service.getFirstConfig();

    assertTrue(config.isPresent());
    var exportConfig = config.get();
    Assertions.assertAll(
      () -> assertEquals("6c163f99-9df5-419d-9174-da638a1c76ed", exportConfig.getId()),
      () -> assertEquals(ExportConfig.SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
      () -> assertEquals(List.of(ExportConfig.WeekDaysEnum.FRIDAY, ExportConfig.WeekDaysEnum.MONDAY), exportConfig.getWeekDays()));
  }

  @Test
  @DisplayName("Config is not set")
  void testGetFirstConfigEmpty() {
    when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

    var config = service.getFirstConfig();

    assertTrue(config.isEmpty());
  }

  @Test
  @DisplayName("Fetch config collection")
  void testGetConfigCollection() {
    when(repository.findByCql(any(), any())).thenReturn(new PageImpl<>(List.of(CONFIG_ENTITY)));

    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 1);

    Assertions.assertAll(
      () -> assertEquals(1, config.getTotalRecords()),
      () -> assertEquals(1, config.getConfigs().size()));

    var exportConfig = config.getConfigs().getFirst();
    Assertions.assertAll(
      () -> assertEquals("6c163f99-9df5-419d-9174-da638a1c76ed", exportConfig.getId()),
      () -> assertEquals(ExportConfig.SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
      () -> assertEquals(List.of(ExportConfig.WeekDaysEnum.FRIDAY, ExportConfig.WeekDaysEnum.MONDAY), exportConfig.getWeekDays()));
  }

  @Test
  @DisplayName("Fetch empty config collection")
  void testGetConfigCollectionEmpty() {
    when(repository.findByCql(any(), any())).thenReturn(Page.empty());

    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 1);

    Assertions.assertAll(
      () -> assertEquals(0, config.getTotalRecords()),
      () -> assertTrue(config.getConfigs().isEmpty()));
  }

}
