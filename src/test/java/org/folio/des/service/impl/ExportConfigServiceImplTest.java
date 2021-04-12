package org.folio.des.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ConfigModel;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.ExportConfig.WeekDaysEnum;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {ExportConfigServiceImpl.class, JacksonConfiguration.class})
class ExportConfigServiceImplTest {

  public static final String CONFIG_RESPONSE =
      "    {\n"
          + "      \"configs\": [\n"
          + "        {\n"
          + "          \"id\": \"d855141f-aa62-40bb-a34b-da986b35d6d4\",\n"
          + "          \"module\": \"mod-bursar-export\",\n"
          + "          \"configName\": \"query_parameters\",\n"
          + "          \"description\": \"for launch job\",\n"
          + "          \"default\": true,\n"
          + "          \"enabled\": true,\n"
          + "          \"value\": \"{\\\"id\\\":\\\"6c163f99-9df5-419d-9174-da638a1c76ed\\\",\\\"schedulePeriod\\\":\\\"DAY\\\",\\\"weekDays\\\":[\\\"FRIDAY\\\",\\\"MONDAY\\\"]}\",\n"
          + "          \"metadata\": {\n"
          + "            \"createdDate\": \"2021-02-04T05:25:19.580+00:00\",\n"
          + "            \"createdByUserId\": \"1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f\",\n"
          + "            \"updatedDate\": \"2021-02-04T06:06:18.875+00:00\",\n"
          + "            \"updatedByUserId\": \"1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f\"\n"
          + "          }\n"
          + "        }\n"
          + "      ],\n"
          + "      \"totalRecords\": 1,\n"
          + "      \"resultInfo\": {\n"
          + "        \"totalRecords\": 1,\n"
          + "        \"facets\": [],\n"
          + "        \"diagnostics\": []\n"
          + "      }\n"
          + "    }";
  public static final String EMPTY_CONFIG_RESPONSE = "{configs: [], totalRecords: 0}";

  @Autowired
  private ExportConfigServiceImpl service;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private ConfigurationClient client;

  @Test
  @DisplayName("Set new configuration")
  void addConfig() throws JsonProcessingException {
    ExportConfig bursarExportConfig = new ExportConfig();
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    BursarFeeFines bursarFeeFines = new BursarFeeFines();
    bursarFeeFines.setDaysOutstanding(9);
    bursarFeeFines.setPatronGroups(List.of(UUID.randomUUID().toString()));
    parameters.setBursarFeeFines(bursarFeeFines);
    ConfigModel mockResponse = mockResponse(bursarExportConfig);
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);

    var response = service.postConfig(bursarExportConfig);

    Assertions.assertAll(
      () -> assertNotNull(response.getId()),
      () -> assertEquals(mockResponse.getConfigName(), response.getConfigName()),
      () -> assertEquals(mockResponse.getModule(), response.getModule()),
      () -> assertEquals(mockResponse.getDescription(), response.getDescription()),
      () -> assertEquals(mockResponse.isDefaultFlag(), response.isDefaultFlag()),
      () -> assertEquals(mockResponse.isEnabled(), response.isEnabled())
    );
  }

  private ConfigModel mockResponse(ExportConfig bursarExportConfig)
    throws JsonProcessingException {
    var mockResponse = new ConfigModel();
    mockResponse.setId(UUID.randomUUID().toString());
    mockResponse.setModule("test_module");
    mockResponse.setConfigName("bursar_config");
    mockResponse.setDescription("test description");
    mockResponse.setEnabled(true);
    mockResponse.setDefaultFlag(true);
    mockResponse.setValue(objectMapper.writeValueAsString(bursarExportConfig));
    return mockResponse;
  }

  @Test
  @DisplayName("Config is not set")
  void noConfig() {
    Mockito.when(client.getConfiguration(any())).thenReturn(EMPTY_CONFIG_RESPONSE);

    var config = service.getConfig();

    Assertions.assertTrue(config.isEmpty());
  }

  @Test
  @DisplayName("Fetch empty config collection")
  void fetchEmptyConfigCollection() {
    Mockito.when(client.getConfiguration(any())).thenReturn(EMPTY_CONFIG_RESPONSE);

    var config = service.getConfigCollection();

    Assertions.assertAll(
        () -> assertEquals(0, config.getTotalRecords()),
        () -> assertTrue(config.getConfigs().isEmpty()));
  }

  @Test
  @DisplayName("Fetch config collection")
  void fetchConfigCollection() {
    Mockito.when(client.getConfiguration(any())).thenReturn(CONFIG_RESPONSE);

    var config = service.getConfigCollection();

    Assertions.assertAll(
        () -> assertEquals(1, config.getTotalRecords()),
        () -> assertEquals(1, config.getConfigs().size()));

    var exportConfig = config.getConfigs().get(0);
    Assertions.assertAll(
        () -> assertEquals("d855141f-aa62-40bb-a34b-da986b35d6d4", exportConfig.getId()),
        () -> assertEquals(SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
        () ->
            assertEquals(
                List.of(WeekDaysEnum.FRIDAY, WeekDaysEnum.MONDAY), exportConfig.getWeekDays()));
  }

  @Test
  @DisplayName("Config exists and parsed correctly")
  void getConfig() {
    Mockito.when(client.getConfiguration(any())).thenReturn(CONFIG_RESPONSE);

    var config = service.getConfig();

    assertTrue(config.isPresent());

    final ExportConfig exportConfig = config.get();

    Assertions.assertAll(
        () -> assertEquals("d855141f-aa62-40bb-a34b-da986b35d6d4", exportConfig.getId()),
        () -> assertEquals(SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
        () ->
            assertEquals(
                List.of(WeekDaysEnum.FRIDAY, WeekDaysEnum.MONDAY), exportConfig.getWeekDays()));
  }
}
