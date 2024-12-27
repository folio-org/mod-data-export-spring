package org.folio.des.service.config.acquisition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.config.scheduling.QuartzSchemaInitializer;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(classes = { DefaultModelConfigToExportConfigConverter.class, JacksonConfiguration.class, ServiceConfiguration.class })
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class })
class ClaimsExportServiceTest {

  public static final String EMPTY_CONFIG_RESPONSE = "{\"configs\": [], \"totalRecords\": 0}";

  @Autowired
  private ClaimsExportService service;
  @MockBean
  private ConfigurationClient client;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private Scheduler scheduler;
  @MockBean
  private QuartzSchemaInitializer quartzSchemaInitializer;

  @Test
  @DisplayName("Set new configuration")
  void addConfig() throws JsonProcessingException {
    var edifactOrdersExportConfig = new ExportConfig();
    edifactOrdersExportConfig.setId(UUID.randomUUID().toString());

    var vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setConfigName("name");
    vendorEdiOrdersExportConfig.vendorId(UUID.randomUUID());
    vendorEdiOrdersExportConfig.integrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    vendorEdiOrdersExportConfig.transmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    vendorEdiOrdersExportConfig.fileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.CSV);

    var parameters = new ExportTypeSpecificParameters();
    parameters.vendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    edifactOrdersExportConfig.setType(ExportType.CLAIMS);
    edifactOrdersExportConfig.exportTypeSpecificParameters(parameters);

    var mockResponse = mockResponse(edifactOrdersExportConfig);
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);

    var response = service.postConfig(edifactOrdersExportConfig);
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
  @DisplayName("Set existing configuration")
  void updateConfig() throws JsonProcessingException {
    var configId = UUID.randomUUID().toString();

    var edifactOrdersExportConfig = new ExportConfig();
    edifactOrdersExportConfig.setId(configId);

    var vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setConfigName("name");
    vendorEdiOrdersExportConfig.vendorId(UUID.randomUUID());
    vendorEdiOrdersExportConfig.integrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    vendorEdiOrdersExportConfig.transmissionMethod(VendorEdiOrdersExportConfig.TransmissionMethodEnum.FILE_DOWNLOAD);
    vendorEdiOrdersExportConfig.fileFormat(VendorEdiOrdersExportConfig.FileFormatEnum.CSV);

    var parameters = new ExportTypeSpecificParameters();
    parameters.vendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    edifactOrdersExportConfig.setType(ExportType.CLAIMS);
    edifactOrdersExportConfig.exportTypeSpecificParameters(parameters);

    var mockResponse = mockResponse(edifactOrdersExportConfig);
    Mockito.when(client.postConfiguration(any())).thenReturn(mockResponse);
    Mockito.doNothing().when(client).putConfiguration(any(), any());

    var postResponse = service.postConfig(edifactOrdersExportConfig);
    Assertions.assertAll(
      () -> assertNotNull(postResponse.getId()),
      () -> assertEquals(mockResponse.getConfigName(), postResponse.getConfigName()),
      () -> assertEquals(mockResponse.getModule(), postResponse.getModule()),
      () -> assertEquals(mockResponse.getDescription(), postResponse.getDescription()),
      () -> assertEquals(mockResponse.getDefault(), postResponse.getDefault()),
      () -> assertEquals(mockResponse.getEnabled(), postResponse.getEnabled())
    );

    vendorEdiOrdersExportConfig.setConfigName("test_claims_config_update");
    service.updateConfig(configId, edifactOrdersExportConfig);
  }

  @Test
  @DisplayName("Config is not set")
  void noConfig() throws JsonProcessingException {
    final var mockedResponse = objectMapper.readValue(EMPTY_CONFIG_RESPONSE, ConfigurationCollection.class);
    Mockito.when(client.getConfigurations(any(), eq(1))).thenReturn(mockedResponse);
    var config = service.getFirstConfig();
    assertTrue(config.isEmpty());
  }

  private ModelConfiguration mockResponse(ExportConfig edifactOrdersExportConfig) throws JsonProcessingException {
    var mockResponse = new ModelConfiguration();
    mockResponse.setId(UUID.randomUUID().toString());
    mockResponse.setModule("test_module");
    mockResponse.setConfigName("test_claims_config");
    mockResponse.setDescription("test description");
    mockResponse.setEnabled(true);
    mockResponse.setDefault(true);
    mockResponse.setValue(objectMapper.writeValueAsString(edifactOrdersExportConfig));

    return mockResponse;
  }
}
