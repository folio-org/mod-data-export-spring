package org.folio.des.converter.acquisition;

import java.util.List;
import java.util.UUID;

import org.folio.des.config.JacksonConfiguration;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.domain.dto.Edi;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.scheduling.ExportTaskTrigger;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.vertx.core.json.JsonObject;

@SpringBootTest(classes = { JacksonConfiguration.class, EdifactOrdersExportParametersValidator.class,
        EdifactOrdersExportConfigToTaskTriggerConverter.class})
public class EdifactOrdersExportConfigToTaskTriggerConverterTest {
  @Autowired EdifactOrdersExportConfigToTaskTriggerConverter converter;

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
    EdiConfig defaultEdiConfig = new EdiConfig();
    Edi defaultEdi = new Edi();
    defaultEdiConfig.setEdi(defaultEdi);
    EdiSchedule ediSchedule = new EdiSchedule();
    ediSchedule.enableScheduledExport(true);
    ScheduleParameters scheduledParameters = new ScheduleParameters();
    scheduledParameters.setId(UUID.randomUUID());
    scheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    scheduledParameters.setScheduleFrequency(7);
    scheduledParameters.setScheduleTime("15:08:39.278+00:00");
    ediSchedule.setScheduleParameters(scheduledParameters);
    defaultEdi.setEdiSchedule(ediSchedule);

    defaultEdiConfig.setAccountNo("def-11111");
    vendorEdiOrdersExportConfig.setDefaultEdiConfig(defaultEdiConfig);

    EdiConfig accountEdiConfig =new EdiConfig();
    Edi accountEdi = new Edi();
    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setId(UUID.randomUUID());
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime("17:08:39.278+00:00");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    accountEdi.setEdiSchedule(accountEdiSchedule);
    accountEdiConfig.setEdi(accountEdi);
    accountEdiConfig.setAccountNo("account-22222");
    vendorEdiOrdersExportConfig.setEdiConfigs(List.of(accountEdiConfig));

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    String export = JsonObject.mapFrom(ediConfig).encodePrettily();
    System.out.println(export);
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(ediConfig);


//    Assertions.assertAll(
//      () -> assertEquals(expId, actConfig.getId()),
//      () -> assertEquals(ExportType.EDIFACT_ORDERS_EXPORT + "_" + vendorId.toString(), actConfig.getConfigName()),
//      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
//      () -> assertEquals(true, actConfig.getDefault()),
//      () -> assertEquals(true, actConfig.getEnabled())
//    );
  }
}
