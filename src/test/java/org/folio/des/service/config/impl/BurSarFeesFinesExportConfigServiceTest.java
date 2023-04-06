package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.UUID;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {DefaultModelConfigToExportConfigConverter.class, JacksonConfiguration.class,
                  ServiceConfiguration.class})
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class, QuartzAutoConfiguration.class})
class BurSarFeesFinesExportConfigServiceTest {

  public static final String CONFIG_RESPONSE =
    """
          {
            "configs": [
              {
                "id": "d855141f-aa62-40bb-a34b-da986b35d6d4",
                "module": "mod-bursar-export",
                "configName": "query_parameters",
                "description": "for launch job",
                "default": true,
                "enabled": true,
                "value": "{\\"id\\":\\"6c163f99-9df5-419d-9174-da638a1c76ed\\",\\"schedulePeriod\\":\\"DAY\\",\\"weekDays\\":[\\"FRIDAY\\",\\"MONDAY\\"]}",
                "metadata": {
                  "createdDate": "2021-02-04T05:25:19.580+00:00",
                  "createdByUserId": "1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f",
                  "updatedDate": "2021-02-04T06:06:18.875+00:00",
                  "updatedByUserId": "1d3b58cb-07b5-5fcd-8a2a-3ce06a0eb90f"
                }
              }
            ],
            "totalRecords": 1,
            "resultInfo": {
              "totalRecords": 1,
              "facets": [],
              "diagnostics": []
            }
          }\
      """;
  public static final String EMPTY_CONFIG_RESPONSE = "{\"configs\": [], \"totalRecords\": 0}";

  @Autowired
  private BurSarFeesFinesExportConfigService service;
  @Autowired
  private DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
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
    bursarExportConfig.exportTypeSpecificParameters(parameters);
    ModelConfiguration mockResponse = mockResponse(bursarExportConfig);
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);

    var response = service.postConfig(bursarExportConfig);

    Assertions.assertAll(
      () -> assertNotNull(response.getId()),
      () -> assertEquals(mockResponse.getConfigName(), response.getConfigName()),
      () -> assertEquals(mockResponse.getModule(), response.getModule()),
      () -> assertEquals(mockResponse.getDescription(), response.getDescription()),
      () -> assertEquals(mockResponse.getDefault(), response.getDefault()),
      () -> assertEquals(mockResponse.getEnabled(), response.getEnabled())
    );
  }

  @Test
  @DisplayName("Should not create new configuration without specific parameters")
  void shouldNorCreateConfigurationAndThroughExceptionIfSpecificParametersIsNotSet() throws JsonProcessingException {
    ExportConfig bursarExportConfig = new ExportConfig();
    ModelConfiguration mockResponse = mockResponse(bursarExportConfig);
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);

    assertThrows(IllegalStateException.class, () ->  service.postConfig(bursarExportConfig));
    Mockito.verify(client, Mockito.times(0)).postConfiguration(any());
  }

  @Test
  @DisplayName("Should not create new configuration without bur sar parameters")
  void shouldNorCreateConfigurationAndThroughExceptionIfBurSarConfigIsNotSet() throws JsonProcessingException {
    ExportConfig bursarExportConfig = new ExportConfig();
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    bursarExportConfig.setExportTypeSpecificParameters(parameters);
    ModelConfiguration mockResponse = mockResponse(bursarExportConfig);
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);

    assertThrows(IllegalArgumentException.class, () -> service.postConfig(bursarExportConfig));
    Mockito.verify(client, Mockito.times(0)).postConfiguration(any());
  }

  private ModelConfiguration mockResponse(ExportConfig bursarExportConfig)
    throws JsonProcessingException {
    var mockResponse = new ModelConfiguration();
    mockResponse.setId(UUID.randomUUID().toString());
    mockResponse.setModule("test_module");
    mockResponse.setConfigName("bursar_config");
    mockResponse.setDescription("test description");
    mockResponse.setEnabled(true);
    mockResponse.setDefault(true);
    mockResponse.setValue(objectMapper.writeValueAsString(bursarExportConfig));
    return mockResponse;
  }

  @Test
  @DisplayName("Config is not set")
  void noConfig() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(EMPTY_CONFIG_RESPONSE, ConfigurationCollection.class);
    Mockito.when(client.getConfigurations(any(), eq(1))).thenReturn(mockedResponse);

    var config = service.getFirstConfig();

    assertTrue(config.isEmpty());
  }

  @Test
  @DisplayName("Fetch empty config collection")
  void fetchEmptyConfigCollection() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(EMPTY_CONFIG_RESPONSE, ConfigurationCollection.class);
    Mockito.when(client.getConfigurations(any(), eq(1))).thenReturn(mockedResponse);

    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 1);

    Assertions.assertAll(
        () -> assertEquals(0, config.getTotalRecords()),
        () -> assertTrue(config.getConfigs().isEmpty()));
  }

  @Test
  @DisplayName("Fetch config collection")
  void fetchConfigCollection() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(CONFIG_RESPONSE, ConfigurationCollection.class);
    Mockito.when(client.getConfigurations(any(), eq(1))).thenReturn(mockedResponse);

    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 1);

    Assertions.assertAll(
        () -> assertEquals(1, config.getTotalRecords()),
        () -> assertEquals(1, config.getConfigs().size()));

    var exportConfig = config.getConfigs().get(0);
    Assertions.assertAll(
        () -> assertEquals("d855141f-aa62-40bb-a34b-da986b35d6d4", exportConfig.getId()),
        () -> assertEquals(ExportConfig.SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
        () ->
            assertEquals(
                List.of(ExportConfig.WeekDaysEnum.FRIDAY, ExportConfig.WeekDaysEnum.MONDAY), exportConfig.getWeekDays()));
  }

  @Test
  @DisplayName("Config exists and parsed correctly")
  void getConfig() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(CONFIG_RESPONSE, ConfigurationCollection.class);
    Mockito.when(client.getConfigurations(any(), eq(1))).thenReturn(mockedResponse);

    var config = service.getFirstConfig();

    assertTrue(config.isPresent());

    final ExportConfig exportConfig = config.get();

    Assertions.assertAll(
        () -> assertEquals("d855141f-aa62-40bb-a34b-da986b35d6d4", exportConfig.getId()),
        () -> assertEquals(ExportConfig.SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
        () ->
            assertEquals(
                List.of(ExportConfig.WeekDaysEnum.FRIDAY, ExportConfig.WeekDaysEnum.MONDAY), exportConfig.getWeekDays()));
  }
}
