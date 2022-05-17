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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = { JacksonConfiguration.class, ServiceConfiguration.class,
                            EdifactOrdersExportConfigToTaskTriggerConverter.class})
class EdifactOrdersExportConfigToTaskTriggerConverterTest {
  @Autowired
  EdifactOrdersExportConfigToTaskTriggerConverter converter;
  @MockBean
  private ConfigurationClient client;

  @Test
  void shouldCreateTriggerIfExportConfigIsValid() {
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
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);
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
    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

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

  @Test
  void shouldCreateTriggerAndSkipReassignIdForScheduledParameter() {
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
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    UUID scheduledId = UUID.randomUUID();
    accScheduledParameters.setId(scheduledId);
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
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
    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

    assertEquals(1, exportTaskTriggers.size());
    ScheduleParameters accScheduleParameters = exportTaskTriggers.get(0).getScheduleParameters();
    Assertions.assertAll(
      () -> assertEquals(scheduledId.toString(), accScheduleParameters.getId().toString()),
      () -> assertEquals(accTime, accScheduleParameters.getScheduleTime()),
      () -> assertEquals(7, accScheduleParameters.getScheduleFrequency()),
      () -> assertEquals("Pacific/Midway", accScheduleParameters.getTimeZone()),
      () -> assertEquals(ScheduleParameters.SchedulePeriodEnum.WEEK, accScheduleParameters.getSchedulePeriod())
    );
  }

  @Test
  void shouldNotCreateTriggerIfShceduledParameterTypeIsNONE() {
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
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
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

    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(exportConfig);

    assertEquals(0, exportTaskTriggers.size());
  }

  @Test
  void shouldThrowExceptionBecauseFtpPortIsNull() {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(expId);
    exportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();

    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    EdiConfig ediConfig =new EdiConfig();
    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.setLibEdiCode("7659876");
    ediConfig.setLibEdiType(EdiConfig.LibEdiTypeEnum._31B_US_SAN);
    ediConfig.setVendorEdiCode("1694510");
    ediConfig.setVendorEdiType(EdiConfig.VendorEdiTypeEnum._31B_US_SAN);

    vendorEdiOrdersExportConfig.setEdiFtp(new EdiFtp());
    vendorEdiOrdersExportConfig.setEdiConfig(ediConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    exportConfig.exportTypeSpecificParameters(parameters);

    Exception exception = assertThrows(IllegalArgumentException.class, () -> converter.convert(exportConfig));

    String expectedMessage = "Export configuration is incomplete, missing FTP/SFTP Port";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void shouldThrowExceptionBecauseVendorEdiCodeIsNull() {
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
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.setLibEdiCode("7659876");
    ediConfig.setLibEdiType(EdiConfig.LibEdiTypeEnum._31B_US_SAN);
    ediConfig.setVendorEdiType(EdiConfig.VendorEdiTypeEnum._31B_US_SAN);
    ediFtp.setFtpPort(22);

    vendorEdiOrdersExportConfig.setEdiFtp(ediFtp);

    vendorEdiOrdersExportConfig.setEdiConfig(ediConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    exportConfig.exportTypeSpecificParameters(parameters);

    Exception exception = assertThrows(IllegalArgumentException.class, () -> converter.convert(exportConfig));

    String expectedMessage = "Export configuration is incomplete, missing library EDI code/Vendor EDI code";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }
}
