package org.folio.des.converter.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.UUID;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.vertx.core.json.JsonObject;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = { JacksonConfiguration.class, ServiceConfiguration.class,
                            EdifactOrdersExportConfigToTaskTriggerConverter.class})
class EdifactOrdersExportConfigToTaskTriggerConverterTest {
  @Autowired
  EdifactOrdersExportConfigToTaskTriggerConverter converter;
  @MockBean
  private ConfigurationClient client;

  @Test
  void testConverterIfExportConfigIsValid() {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();

    vendorEdiOrdersExportConfig.setVendorId(vendorId);


    EdiConfig accountEdiConfig =new EdiConfig();
    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    String accTime = "17:08:39.278+00:00";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);
    accountEdiConfig.addAccountNoListItem("account-22222");
    vendorEdiOrdersExportConfig.setEdiConfig(accountEdiConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    ConfigurationCollection configurationCollection = new ConfigurationCollection();
    ModelConfiguration modelConfiguration = new ModelConfiguration().id(expId)
      .module("ORG")
      .configName(ExportType.EDIFACT_ORDERS_EXPORT.getValue() + "_" + vendorId + "_" + expId)
      .value("{\"locale\":\"en-US\",\"timezone\":\"Pacific/Midway\",\"currency\":\"USD\"}");
    configurationCollection.setConfigs(List.of(modelConfiguration));
    Mockito.doReturn(configurationCollection).when(client).getConfigurations(any());
    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(ediConfig);

    assertEquals(1, exportTaskTriggers.size());
    ScheduleParameters accScheduleParameters = exportTaskTriggers.get(0).getScheduleParameters();
    Assertions.assertAll(
      () -> assertNotNull(accScheduleParameters.getId()),
      () -> assertEquals(accTime, accScheduleParameters.getScheduleTime()),
      () -> assertEquals(7, accScheduleParameters.getScheduleFrequency()),
      () -> assertEquals("Pacific/Midway", accScheduleParameters.getTimeZone()),
      () -> assertEquals(ScheduleParameters.SchedulePeriodEnum.WEEK, accScheduleParameters.getSchedulePeriod())
    );
  }
}
