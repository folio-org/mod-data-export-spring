package org.folio.des.scheduling.quartz.converter.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverter;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverterImpl;
import org.folio.des.scheduling.quartz.converter.acquisition.ExportConfigToEdifactTriggerConverter;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.folio.des.validator.acquisition.EdifactOrdersScheduledParamsValidator;
import org.junit.jupiter.api.Test;

class ExportConfigToEdifactTriggerConverterTest {
  private static final String ACC_TIME = "17:08:39";
  private final EdifactOrdersScheduledParamsValidator scheduledParamsValidator = new EdifactOrdersScheduledParamsValidator();
  private final EdifactOrdersExportParametersValidator exportParamsValidator = new EdifactOrdersExportParametersValidator(scheduledParamsValidator);
  private final ScheduleParametersToTriggerConverter scheduleParamsToTriggerConverter = new ScheduleParametersToTriggerConverterImpl();
  private final ExportConfigToEdifactTriggerConverter converter
    = new ExportConfigToEdifactTriggerConverter(exportParamsValidator, scheduleParamsToTriggerConverter);

  @Test
  void shouldCreateEnabledTrigger() {
    ExportConfig exportConfig = getExportConfig();

    var exportTrigger = converter.convert(exportConfig);
    assertNotNull(exportTrigger);
    assertFalse(exportTrigger.isDisabled());

    var triggers = exportTrigger.triggers();
    assertNotNull(triggers);
    assertEquals(1, triggers.size());
    assertEquals("edifact_orders_export", triggers.iterator().next().getKey().getGroup());
  }

  @Test
  void shouldCreateDisabledExportTriggerIfScheduledParameterTypeIsNone() {
    // we need to create trigger even if scheduler period is NONE, because this trigger is required
    // to delete existing scheduler task for this case
    ExportConfig exportConfig = getExportConfig();
    ScheduleParameters scheduleParams = exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters();

    scheduleParams.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);

    var exportTrigger = converter.convert(exportConfig);
    assertNotNull(exportTrigger);
    assertTrue(exportTrigger.isDisabled());
    assertTrue(CollectionUtils.isEmpty(exportTrigger.triggers()));
  }

  @Test
  void shouldCreateDisabledExportTriggerIfSchedulerDisabled() {
    // we need to create trigger even scheduler disabled, because this trigger is required to delete
    // existing scheduler task for this case
    ExportConfig exportConfig = getExportConfig();
    EdiSchedule ediSchedule = exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule();

    ediSchedule.setEnableScheduledExport(false);

    var exportTrigger = converter.convert(exportConfig);
    assertNotNull(exportTrigger);
    assertTrue(exportTrigger.isDisabled());
    assertTrue(CollectionUtils.isEmpty(exportTrigger.triggers()));
  }

  @Test
  void shouldCreateEnabledExportTriggerWithoutTriggersIfSchedulerParamsAreNull() {
    ExportConfig exportConfig = getExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .scheduleParameters(null);

    var exportTrigger = converter.convert(exportConfig);
    assertNotNull(exportTrigger);
    assertFalse(exportTrigger.isDisabled());
    assertTrue(CollectionUtils.isEmpty(exportTrigger.triggers()));
  }

  @Test
  void shouldThrowExceptionIfSchedulerTimeIsNull() {
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
    accScheduledParameters.setWeekDays(List.of(ScheduleParameters.WeekDaysEnum.THURSDAY));
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
