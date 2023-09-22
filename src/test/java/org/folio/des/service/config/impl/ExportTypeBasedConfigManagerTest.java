package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doNothing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.config.scheduling.QuartzSchemaInitializer;
import org.folio.des.domain.dto.BursarExportFilter;
import org.folio.des.domain.dto.BursarExportFilterAge;
import org.folio.des.domain.dto.BursarExportFilterCondition;
import org.folio.des.domain.dto.BursarExportFilterCondition.OperationEnum;
import org.folio.des.domain.dto.BursarExportFilterPatronGroup;
import org.folio.des.domain.dto.BursarExportJob;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfig.SchedulePeriodEnum;
import org.folio.des.domain.dto.ExportConfig.WeekDaysEnum;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.exception.RequestValidationException;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = { ServiceConfiguration.class })
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class })
class ExportTypeBasedConfigManagerTest {

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
  public static final String EMPTY_CONFIG_RESPONSE =
    "{\"configs\": [], \"totalRecords\": 0}";

  @Autowired
  private ExportTypeBasedConfigManager service;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ConfigurationClient client;

  @MockBean
  private Scheduler scheduler;

  @MockBean
  private QuartzSchemaInitializer quartzSchemaInitializer;

  @MockBean
  private BursarExportScheduler bursarExportScheduler;

  @ParameterizedTest
  @CsvSource(
    {
      "BATCH_VOUCHER_EXPORT, BATCH_VOUCHER_EXPORT",
      "BURSAR_FEES_FINES, export_config_parameters",
    }
  )
  @DisplayName("Set new configuration")
  void addConfig(ExportType exportType, String exportName)
    throws JsonProcessingException {
    ExportConfig bursarExportConfig = new ExportConfig();
    bursarExportConfig.setType(exportType);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();

    BursarExportJob bursarFeeFines = new BursarExportJob();
    BursarExportFilterAge bursarExportFilterAge = new BursarExportFilterAge();
    bursarExportFilterAge.setNumDays(1);
    BursarExportFilterPatronGroup bursarExportFilterPatronGroup = new BursarExportFilterPatronGroup();
    bursarExportFilterPatronGroup.setPatronGroupId(
      UUID.fromString("0000-00-00-00-000000")
    );
    List<BursarExportFilter> bursarExportFilters = new ArrayList<>();
    bursarExportFilters.add(bursarExportFilterPatronGroup);
    bursarExportFilters.add(bursarExportFilterAge);
    BursarExportFilterCondition bursarExportFilterCondition = new BursarExportFilterCondition();
    bursarExportFilterCondition.setCriteria(bursarExportFilters);
    bursarExportFilterCondition.setOperation(OperationEnum.AND);
    bursarFeeFines.setFilter(bursarExportFilterCondition);
    parameters.setBursarFeeFines(bursarFeeFines);
    bursarExportConfig.exportTypeSpecificParameters(parameters);
    ModelConfiguration mockResponse = mockResponse(
      bursarExportConfig,
      exportName
    );
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);
    doNothing().when(bursarExportScheduler).scheduleBursarJob(any());
    doNothing().when(bursarExportScheduler).scheduleBursarJob(any());

    var response = service.postConfig(bursarExportConfig);

    Assertions.assertAll(
      () -> assertNotNull(response.getId()),
      () -> assertEquals(mockResponse.getConfigName(), exportName),
      () -> assertEquals(mockResponse.getModule(), response.getModule()),
      () ->
        assertEquals(mockResponse.getDescription(), response.getDescription()),
      () -> assertEquals(mockResponse.getDefault(), response.getDefault()),
      () -> assertEquals(mockResponse.getEnabled(), response.getEnabled())
    );
    Mockito.verify(client, Mockito.times(1)).postConfiguration(any());
  }

  @Test
  @DisplayName(
    "Should not create new configuration without specific parameters"
  )
  void shouldNorCreateConfigurationAndThroughExceptionIfSpecificParametersIsNotSet()
    throws JsonProcessingException {
    ExportConfig bursarExportConfig = new ExportConfig();
    ModelConfiguration mockResponse = mockResponse(
      bursarExportConfig,
      "export_config_parameters"
    );
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);

    assertThrows(
      IllegalStateException.class,
      () -> service.postConfig(bursarExportConfig)
    );
    Mockito.verify(client, Mockito.times(0)).postConfiguration(any());
  }

  @Test
  @DisplayName("Should throw RequestValidationException")
  void shouldThrowRequestValidationException() {
    ExportConfig exportConfig = new ExportConfig();
    Exception exception = assertThrows(
      RequestValidationException.class,
      () -> service.updateConfig(null, exportConfig)
    );

    String expectedMessage = "Mismatch between id in path and request body";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  @DisplayName("Should not create new configuration without bur sar parameters")
  void shouldNorCreateConfigurationAndThroughExceptionIfBurSarConfigIsNotSet()
    throws JsonProcessingException {
    ExportConfig bursarExportConfig = new ExportConfig();
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    bursarExportConfig.setExportTypeSpecificParameters(parameters);
    ModelConfiguration mockResponse = mockResponse(
      bursarExportConfig,
      "export_config_parameters"
    );
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);

    assertThrows(
      IllegalArgumentException.class,
      () -> service.postConfig(bursarExportConfig)
    );
    Mockito.verify(client, Mockito.times(0)).postConfiguration(any());
  }

  private ModelConfiguration mockResponse(
    ExportConfig bursarExportConfig,
    String configName
  ) throws JsonProcessingException {
    var mockResponse = new ModelConfiguration();
    mockResponse.setId(UUID.randomUUID().toString());
    mockResponse.setModule("test_module");
    mockResponse.setConfigName(configName);
    mockResponse.setDescription("test description");
    mockResponse.setEnabled(true);
    mockResponse.setDefault(true);
    mockResponse.setValue(objectMapper.writeValueAsString(bursarExportConfig));
    return mockResponse;
  }

  @Test
  @DisplayName("Config is not set")
  void noConfig() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(
      EMPTY_CONFIG_RESPONSE,
      ConfigurationCollection.class
    );
    Mockito
      .when(client.getConfigurations(any(), any()))
      .thenReturn(mockedResponse);

    var configs = service.getConfigCollection(null, 10);

    assertTrue(configs.getConfigs().isEmpty());
  }

  @Test
  @DisplayName("Fetch empty config collection")
  void fetchEmptyConfigCollection() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(
      EMPTY_CONFIG_RESPONSE,
      ConfigurationCollection.class
    );
    Mockito
      .when(client.getConfigurations(any(), eq(10)))
      .thenReturn(mockedResponse);

    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 10);

    Assertions.assertAll(
      () -> assertEquals(0, config.getTotalRecords()),
      () -> assertTrue(config.getConfigs().isEmpty())
    );
  }

  @Test
  @DisplayName("Fetch config collection")
  void fetchConfigCollection() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(
      CONFIG_RESPONSE,
      ConfigurationCollection.class
    );
    Mockito
      .when(client.getConfigurations(any(), eq(10)))
      .thenReturn(mockedResponse);

    var query = String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME);
    var config = service.getConfigCollection(query, 10);

    Assertions.assertAll(
      () -> assertEquals(1, config.getTotalRecords()),
      () -> assertEquals(1, config.getConfigs().size())
    );

    var exportConfig = config.getConfigs().get(0);
    Assertions.assertAll(
      () ->
        assertEquals(
          "d855141f-aa62-40bb-a34b-da986b35d6d4",
          exportConfig.getId()
        ),
      () ->
        assertEquals(SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
      () ->
        assertEquals(
          List.of(WeekDaysEnum.FRIDAY, WeekDaysEnum.MONDAY),
          exportConfig.getWeekDays()
        )
    );
  }

  @Test
  @DisplayName("Config exists and parsed correctly")
  void getConfig() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(
      CONFIG_RESPONSE,
      ConfigurationCollection.class
    );
    Mockito
      .when(client.getConfigurations(any(), eq(10)))
      .thenReturn(mockedResponse);

    var configs = service.getConfigCollection(null, 10);

    assertEquals(1, configs.getTotalRecords());

    final ExportConfig exportConfig = configs.getConfigs().get(0);

    Assertions.assertAll(
      () ->
        assertEquals(
          "d855141f-aa62-40bb-a34b-da986b35d6d4",
          exportConfig.getId()
        ),
      () ->
        assertEquals(SchedulePeriodEnum.DAY, exportConfig.getSchedulePeriod()),
      () ->
        assertEquals(
          List.of(WeekDaysEnum.FRIDAY, WeekDaysEnum.MONDAY),
          exportConfig.getWeekDays()
        )
    );
  }
}
