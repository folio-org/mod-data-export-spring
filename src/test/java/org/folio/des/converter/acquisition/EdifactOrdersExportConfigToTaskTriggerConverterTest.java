package org.folio.des.converter.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = { JacksonConfiguration.class, ServiceConfiguration.class,
                            EdifactOrdersExportConfigToTaskTriggerConverter.class})
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
class EdifactOrdersExportConfigToTaskTriggerConverterTest {
  private static final String ACC_TIME = "17:08:39";

  @Autowired
  EdifactOrdersExportConfigToTaskTriggerConverter converter;
  @MockBean
  private ConfigurationClient client;

  @Test
  void shouldCreateTriggerIfExportConfigIsValid() {
    ExportConfig exportConfig = getExportConfig();

    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

    assertEquals(1, exportTaskTriggers.size());
    ScheduleParameters accScheduleParameters = exportTaskTriggers.get(0).getScheduleParameters();
    Assertions.assertAll(
      () -> assertNotNull(accScheduleParameters.getId()),
      () -> assertEquals(ACC_TIME, accScheduleParameters.getScheduleTime()),
      () -> assertEquals(7, accScheduleParameters.getScheduleFrequency()),
      () -> assertEquals("Pacific/Midway", accScheduleParameters.getTimeZone()),
      () -> assertEquals(ScheduleParameters.SchedulePeriodEnum.WEEK, accScheduleParameters.getSchedulePeriod())
    );
  }

  @Test
  void shouldCreateTriggerAndSkipReassignIdForScheduledParameter() {
    ExportConfig exportConfig = getExportConfig();

    UUID schedulerId = UUID.randomUUID();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters()
      .setId(schedulerId);

    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

    assertEquals(1, exportTaskTriggers.size());
    ScheduleParameters accScheduleParameters = exportTaskTriggers.get(0).getScheduleParameters();
    Assertions.assertAll(
      () -> assertEquals(schedulerId.toString(), accScheduleParameters.getId().toString()),
      () -> assertEquals(ACC_TIME, accScheduleParameters.getScheduleTime()),
      () -> assertEquals(7, accScheduleParameters.getScheduleFrequency()),
      () -> assertEquals("Pacific/Midway", accScheduleParameters.getTimeZone()),
      () -> assertEquals(ScheduleParameters.SchedulePeriodEnum.WEEK, accScheduleParameters.getSchedulePeriod())
    );
  }

  @Test
  void shouldCreateTriggerIfScheduledParameterTypeIsNONEButScheduleTimeSet() {
    // we need to create trigger even if scheduler period is NONE, because this trigger is required to delete existing scheduler task for this case
    ExportConfig exportConfig = getExportConfig();
    ScheduleParameters scheduleParams = exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters();

    scheduleParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
    scheduleParams.setScheduleTime(ACC_TIME);

    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

    assertEquals(1, exportTaskTriggers.size());
  }

  @Test
  void shouldCreateTriggerIfSchedulerDisabledButScheduleTimeSet() {
    // we need to create trigger even scheduler disabled, because this trigger is required to delete existing scheduler task for this case
    ExportConfig exportConfig = getExportConfig();
    EdiSchedule ediSchedule = exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule();

    ediSchedule.setEnableScheduledExport(false);
    ediSchedule.getScheduleParameters().setScheduleTime(ACC_TIME);

    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

    assertEquals(1, exportTaskTriggers.size());
  }

  @Test
  void shouldNotCreateTriggerIfSchedulerParamsAreNull() {
    ExportConfig exportConfig = getExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .scheduleParameters(null);

    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

    assertEquals(0, exportTaskTriggers.size());
  }

  @Test
  void shouldNotCreateTriggerIfSchedulerTimeIsNull() {
    ExportConfig exportConfig = getExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters()
      .scheduleTime(null);

    assertThrows(IllegalArgumentException.class, () -> converter.convert(exportConfig),
      "EDIFACT_ORDERS_EXPORT scheduled parameters should contain scheduled time");
  }

  @Test
  void shouldThrowExceptionBecauseFtpPortIsNull() {
    ExportConfig exportConfig = getExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiFtp()
      .setFtpPort(null);

    Exception exception = assertThrows(IllegalArgumentException.class, () -> converter.convert(exportConfig));

    String expectedMessage = "Export configuration is incomplete, missing FTP/SFTP Port";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void shouldThrowExceptionBecauseVendorEdiCodeIsNull() {
    ExportConfig exportConfig = getExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiConfig()
      .setVendorEdiCode(null);

    Exception exception = assertThrows(IllegalArgumentException.class, () -> converter.convert(exportConfig));

    String expectedMessage = "Export configuration is incomplete, missing library EDI code/Vendor EDI code";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  private ExportConfig getExportConfig() {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(expId);
    exportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    EdiFtp ediFtp = new EdiFtp();

    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    EdiConfig ediConfig =new EdiConfig();
    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(ACC_TIME);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.setLibEdiCode("7659876");
    ediConfig.setLibEdiType(EdiConfig.LibEdiTypeEnum._31B_US_SAN);
    ediConfig.setVendorEdiCode("1694510");
    ediConfig.setVendorEdiType(EdiConfig.VendorEdiTypeEnum._31B_US_SAN);
    ediFtp.setFtpPort(22);

    vendorEdiOrdersExportConfig.setEdiFtp(ediFtp);

    vendorEdiOrdersExportConfig.setEdiConfig(ediConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    exportConfig.exportTypeSpecificParameters(parameters);
    return exportConfig;
  }
}
