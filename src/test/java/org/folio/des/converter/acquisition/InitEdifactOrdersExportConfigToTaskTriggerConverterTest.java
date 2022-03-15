package org.folio.des.converter.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.converter.aqcuisition.InitEdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.domain.dto.Metadata;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.folio.des.service.JobService;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = { JacksonConfiguration.class, ServiceConfiguration.class,
                            EdifactOrdersExportConfigToTaskTriggerConverter.class})
class InitEdifactOrdersExportConfigToTaskTriggerConverterTest {
  @Autowired
  InitEdifactOrdersExportConfigToTaskTriggerConverter converter;
  @MockBean
  private ConfigurationClient client;
  @MockBean
  private EdifactOrdersExportParametersValidator validator;
  @MockBean
  private JobService jobService;

  @Test
  void shouldCreateTriggerIfExportConfigIsValid() {
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
    String accTime = "17:08:39";
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.WEEK);
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(accTime);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);
    accountEdiConfig.addAccountNoListItem("account-22222");
    vendorEdiOrdersExportConfig.setEdiConfig(accountEdiConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);
    JobCollection jobCollection = new JobCollection();
    Job job = new Job();
    Metadata metadata = new Metadata();
    metadata.setCreatedDate(new Date());
    job.setExportTypeSpecificParameters(parameters);
    job.setMetadata(metadata);
    jobCollection.addJobRecordsItem(job);
    jobCollection.totalRecords(1);

    doReturn(jobCollection).when(jobService).get(anyInt(), anyInt(), anyString());
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

  @Test
  void shouldCreateTriggerAndSkipReassignIdForScheduledParameter() {
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
    accountEdiConfig.addAccountNoListItem("account-22222");
    vendorEdiOrdersExportConfig.setEdiConfig(accountEdiConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);
    JobCollection jobCollection = new JobCollection();
    Job job = new Job();
    Metadata metadata = new Metadata();
    metadata.setCreatedDate(new Date());
    job.setExportTypeSpecificParameters(parameters);
    job.setMetadata(metadata);
    jobCollection.addJobRecordsItem(job);
    jobCollection.totalRecords(1);

    doReturn(jobCollection).when(jobService).get(anyInt(), anyInt(), anyString());
    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(ediConfig);

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
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();

    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    EdiConfig accountEdiConfig =new EdiConfig();
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
    accountEdiConfig.addAccountNoListItem("account-22222");
    vendorEdiOrdersExportConfig.setEdiConfig(accountEdiConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);
    JobCollection jobCollection = new JobCollection();
    Job job = new Job();
    Metadata metadata = new Metadata();
    metadata.setCreatedDate(new Date());
    job.setExportTypeSpecificParameters(parameters);
    job.setMetadata(metadata);
    jobCollection.addJobRecordsItem(job);
    jobCollection.totalRecords(1);

    doReturn(jobCollection).when(jobService).get(anyInt(), anyInt(), anyString());
    //When
    List<ExportTaskTrigger> exportTaskTriggers = converter.convert(ediConfig);

    assertEquals(0, exportTaskTriggers.size());
  }
}
