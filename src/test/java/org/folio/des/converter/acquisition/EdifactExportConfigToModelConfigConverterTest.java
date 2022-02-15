package org.folio.des.converter.acquisition;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.converter.aqcuisition.EdifactExportConfigToModelConfigConverter;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.folio.des.validator.acquisition.EdifactOrdersScheduledParamsValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { JacksonConfiguration.class, EdifactExportConfigToModelConfigConverter.class,
                            EdifactOrdersExportParametersValidator.class, EdifactOrdersScheduledParamsValidator.class})
class EdifactExportConfigToModelConfigConverterTest {
  @Autowired
  private EdifactExportConfigToModelConfigConverter converter;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testConverterIfExportConfigIsValidAndScheduledIdIsNotProvided() throws JsonProcessingException {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.setScheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    ModelConfiguration actConfig = converter.convert(ediConfig);
    var actExportConfig = objectMapper.readValue(actConfig.getValue(), ExportConfig.class);
    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(ExportType.EDIFACT_ORDERS_EXPORT + "_" + vendorId + "_" + expId, actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled()),
      () -> assertEquals(expId, actExportConfig.getId())
    );
  }

  @Test
  void testConverterIfExportConfigIsValidAndScheduledIdIsProvidedButNotEqualToExportId() throws JsonProcessingException {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setId(UUID.randomUUID());
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    ModelConfiguration actConfig = converter.convert(ediConfig);
    var actExportConfig = objectMapper.readValue(actConfig.getValue(), ExportConfig.class);
    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(ExportType.EDIFACT_ORDERS_EXPORT + "_" + vendorId + "_" + expId, actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled()),
      () -> assertEquals(expId, actExportConfig.getId())
    );
  }

  @ParameterizedTest
  @CsvSource({
    "EDIFACT_ORDERS_EXPORT, EDIFACT_ORDERS_EXPORT"
  })
  void testConverterIfExportConfigIsValidAndTypeIsProvided(ExportType exportType) {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setType(exportType);
    ediConfig.setId(expId);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();

    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    ediConfig.exportTypeSpecificParameters(parameters);

    ModelConfiguration actConfig = converter.convert(ediConfig);

    Assertions.assertAll(
      () -> assertEquals(expId, actConfig.getId()),
      () -> assertEquals(ExportType.EDIFACT_ORDERS_EXPORT + "_" + vendorId + "_" + expId, actConfig.getConfigName()),
      () -> assertEquals(DEFAULT_MODULE_NAME, actConfig.getModule()),
      () -> assertEquals(true, actConfig.getDefault()),
      () -> assertEquals(true, actConfig.getEnabled())
    );
  }

  @ParameterizedTest
  @CsvSource({
    "EDIFACT_ORDERS_EXPORT"
  })
  void shouldThrowExceptionIfExportConfigIsNotValidAndTypeIsProvided(ExportType exportType) {
    String expId = UUID.randomUUID().toString();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setType(exportType);
    ediConfig.setId(expId);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();

    BursarFeeFines bursarFeeFines = new BursarFeeFines();
    bursarFeeFines.setDaysOutstanding(9);
    bursarFeeFines.setPatronGroups(List.of(UUID.randomUUID().toString()));
    parameters.setBursarFeeFines(bursarFeeFines);

    ediConfig.exportTypeSpecificParameters(parameters);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                  () -> converter.convert(ediConfig));
  }
}
