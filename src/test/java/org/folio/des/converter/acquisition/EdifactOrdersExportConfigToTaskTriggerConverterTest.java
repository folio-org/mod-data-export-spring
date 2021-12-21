package org.folio.des.converter.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.junit.jupiter.api.Assertions;
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
    UUID defId = UUID.randomUUID();
    String defTime = "15:08:39.278+00:00";
    ScheduleParameters scheduledParameters = new ScheduleParameters();
    scheduledParameters.setId(defId);
    scheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.DAY);
    scheduledParameters.setScheduleFrequency(2);
    scheduledParameters.setScheduleTime(defTime);
    ediSchedule.setScheduleParameters(scheduledParameters);
    defaultEdi.setEdiSchedule(ediSchedule);

    String defAccNo = "def-11111";
    defaultEdiConfig.setAccountNo(defAccNo);
    vendorEdiOrdersExportConfig.setDefaultEdiConfig(defaultEdiConfig);

    EdiConfig accountEdiConfig =new EdiConfig();
    Edi accountEdi = new Edi();
    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    String accTime = "17:08:39.278+00:00";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
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

    assertEquals(2, exportTaskTriggers.size());
    ScheduleParameters defScheduleParameters = exportTaskTriggers.stream()
                      .map(ExportTaskTrigger::getScheduleParameters)
                      .filter(par -> defId.equals(par.getId())).findAny().get();
    Assertions.assertAll(
      () -> assertEquals(defId, defScheduleParameters.getId()),
      () -> assertEquals(defTime, defScheduleParameters.getScheduleTime()),
      () -> assertEquals(2, defScheduleParameters.getScheduleFrequency()),
      () -> assertEquals(ScheduleParameters.SchedulePeriodEnum.DAY, defScheduleParameters.getSchedulePeriod())
    );
    ScheduleParameters accScheduleParameters = exportTaskTriggers.stream()
      .map(ExportTaskTrigger::getScheduleParameters)
      .filter(par -> !defId.equals(par.getId())).findAny().get();
    Assertions.assertAll(
      () -> assertNotNull(accScheduleParameters.getId()),
      () -> assertEquals(accTime, accScheduleParameters.getScheduleTime()),
      () -> assertEquals(7, accScheduleParameters.getScheduleFrequency()),
      () -> assertEquals(ScheduleParameters.SchedulePeriodEnum.WEEK, accScheduleParameters.getSchedulePeriod())
    );
  }
}
