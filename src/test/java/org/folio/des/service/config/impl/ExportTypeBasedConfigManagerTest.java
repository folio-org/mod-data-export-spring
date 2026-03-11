package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;
import static org.folio.des.support.TestUtils.getBursarExportConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.exception.RequestValidationException;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=diku_mod_data_export_spring")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "classpath:init.sql")
class ExportTypeBasedConfigManagerTest extends BaseTest {

  @MockitoSpyBean
  private ExportConfigRepository repository;
  @Autowired
  private ExportTypeBasedConfigManager service;
  @MockitoBean
  private BursarExportScheduler bursarExportScheduler;

  @AfterEach
  void tearDown() {
    repository.deleteAll();
  }

  @ParameterizedTest
  @ValueSource(strings = {"BATCH_VOUCHER_EXPORT", "BURSAR_FEES_FINES"})
  @DisplayName("Set new configuration")
  void testPostConfig(ExportType exportType) {
    var exportConfig = getBursarExportConfig().tenant("diku").type(exportType)
      .configName(exportType == ExportType.BURSAR_FEES_FINES ? DEFAULT_CONFIG_NAME : exportType.getValue());

    var response = service.postConfig(exportConfig);

    assertEquals(exportConfig, response);
    verify(repository).save(any());
  }

  @Test
  @DisplayName("Should not create new configuration without specific parameters")
  void testPostConfigWithoutSpecificParameters() {
    var exportConfig = new ExportConfig();

    assertThrows(IllegalStateException.class, () -> service.postConfig(exportConfig));

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw RequestValidationException")
  void testPostConfigThrowRequestValidationException() {
    var exportConfig = new ExportConfig();
    var expectedMessage = "Mismatch between id in path and request body";

    var exception = assertThrows(RequestValidationException.class, () -> service.updateConfig(null, exportConfig));

    assertTrue(exception.getMessage().contains(expectedMessage));
  }

  @Test
  @DisplayName("Should not create new configuration without bursar parameters")
  void testPostConfigWithoutBursarParameters() {
    var bursarExportConfig = new ExportConfig().exportTypeSpecificParameters(new ExportTypeSpecificParameters());

    assertThrows(IllegalArgumentException.class, () -> service.postConfig(bursarExportConfig));

    verify(repository, times(0)).save(any());
  }

  @Test
  @DisplayName("Fetch config collection")
  void testGetConfigCollection() {
    var id = UUID.randomUUID().toString();
    var bursarConfig = getBursarExportConfig().id(id)
      .tenant("diku")
      .schedulePeriod(ExportConfig.SchedulePeriodEnum.DAY)
      .weekDays(List.of(ExportConfig.WeekDaysEnum.FRIDAY, ExportConfig.WeekDaysEnum.MONDAY));
    service.postConfig(bursarConfig);

    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 10);

    Assertions.assertAll(
      () -> assertEquals(1, config.getTotalRecords()),
      () -> assertEquals(1, config.getConfigs().size()));

    var exportConfig = config.getConfigs().getFirst();
    Assertions.assertAll(
      () -> assertEquals(id, exportConfig.getId()),
      () -> assertEquals(ExportConfig.SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
      () -> assertEquals(List.of(ExportConfig.WeekDaysEnum.FRIDAY, ExportConfig.WeekDaysEnum.MONDAY), exportConfig.getWeekDays()));
  }

  @Test
  @DisplayName("Fetch empty config collection")
  void testGetConfigCollectionEmpty() {
    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 10);

    Assertions.assertAll(
      () -> assertEquals(0, config.getTotalRecords()),
      () -> assertTrue(config.getConfigs().isEmpty()));
  }

}
