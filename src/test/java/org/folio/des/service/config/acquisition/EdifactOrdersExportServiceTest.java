package org.folio.des.service.config.acquisition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(classes = {DefaultModelConfigToExportConfigConverter.class, JacksonConfiguration.class,
  ServiceConfiguration.class})
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class, QuartzAutoConfiguration.class})
class EdifactOrdersExportServiceTest {

  public static final String EMPTY_CONFIG_RESPONSE = "{\"configs\": [], \"totalRecords\": 0}";

  @Autowired
  private EdifactOrdersExportService service;
  @MockBean
  private ConfigurationClient client;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private EdifactOrdersExportConfigToTaskTriggerConverter converter;


  @Test
  @DisplayName("Set new configuration")
  void addConfig() throws JsonProcessingException {
    ExportConfig edifactOrdersExportConfig = new ExportConfig();
    edifactOrdersExportConfig.setId(UUID.randomUUID().toString());
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    EdiSchedule ediSchedule = new EdiSchedule();
    ScheduleParameters scheduleParameters = new ScheduleParameters();

    scheduleParameters.scheduleFrequency(1);
    scheduleParameters.schedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR);
    scheduleParameters.scheduleTime("15:00:00");

    ediSchedule.enableScheduledExport(true);
    ediSchedule.scheduleParameters(scheduleParameters);

    vendorEdiOrdersExportConfig.setConfigName("edi_test_config");
    vendorEdiOrdersExportConfig.vendorId(UUID.fromString("046b6c7f-0b8a-43b9-b35d-6489e6daee91"));
    vendorEdiOrdersExportConfig.ediSchedule(ediSchedule);


    parameters.vendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    edifactOrdersExportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    edifactOrdersExportConfig.exportTypeSpecificParameters(parameters);
    ModelConfiguration mockResponse = mockResponse(edifactOrdersExportConfig);
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
  @DisplayName("Config is not set")
  void noConfig() throws JsonProcessingException {
    final ConfigurationCollection mockedResponse = objectMapper.readValue(EMPTY_CONFIG_RESPONSE, ConfigurationCollection.class);
    Mockito.when(client.getConfigurations(any(), eq(1))).thenReturn(mockedResponse);

    var config = service.getFirstConfig();

    assertTrue(config.isEmpty());
  }

  private ModelConfiguration mockResponse(ExportConfig edifactOrdersExportConfig)
    throws JsonProcessingException {
    var mockResponse = new ModelConfiguration();
    mockResponse.setId(UUID.randomUUID().toString());
    mockResponse.setModule("test_module");
    mockResponse.setConfigName("edifact_orders_Config");
    mockResponse.setDescription("test description");
    mockResponse.setEnabled(true);
    mockResponse.setDefault(true);
    mockResponse.setValue(objectMapper.writeValueAsString(edifactOrdersExportConfig));
    return mockResponse;
  }

}
