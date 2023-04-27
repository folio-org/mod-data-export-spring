package org.folio.des.scheduling.quartz.converter.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.quartz.job.ScheduledJobDetails;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

class ExportConfigToEdifactJobDetailConverterTest {
  private static final String EXPORT_CONFIG_ID = "3962583c-7b88-4000-be7d-5e1fe58a3416"/*UUID.randomUUID().toString()*/;
  private static final String SCHEDULE_ID = "8d9626bb-b507-4aeb-a115-8b3d26bbc221"/*UUID.randomUUID().toString()*/;
  private static final ExportType EXPORT_TYPE = ExportType.EDIFACT_ORDERS_EXPORT;
  private static final String TENANT = "some_test_tenant";
  private static final String JOB_ID = UUID.randomUUID().toString();
  private static final String JOB_STRING_VALUE = "{\"isSystemSource\":true,\"tenant\":\"some_test_tenant\"," +
    "\"type\":\"EDIFACT_ORDERS_EXPORT\",\"exportTypeSpecificParameters\":{\"vendorEdiOrdersExportConfig\":" +
    "{\"ediSchedule\":{\"enableScheduledExport\":false,\"scheduleParameters\":{\"id\":" +
    "\"8d9626bb-b507-4aeb-a115-8b3d26bbc221\",\"timeZone\":\"UTC\"}},\"isDefaultConfig\":false}}}";
  ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
  ExportConfigToEdifactJobDetailConverter converter = new ExportConfigToEdifactJobDetailConverter(/*objectMapper*/);

  @Test
  void testConvertNullReturnsNull() {
    assertNull(converter.convert(null, null));
  }

  @Test
  void testConvertExportConfigToJobDetailSuccessful() throws Exception {
    ExportConfig exportConfig = buildExportConfig();
    ScheduledJobDetails scheduledJobDetails = converter.convert(exportConfig, JobKey.jobKey(JOB_ID));

    assertNotNull(scheduledJobDetails);

    Job job = scheduledJobDetails.job();
    System.out.println(objectMapper.writeValueAsString(job));
    /*assertNotNull(job);
    assertNull(job.getId());
    assertEquals(EXPORT_TYPE, job.getType());
    assertEquals(TENANT, job.getTenant());
    assertEquals(true, job.getIsSystemSource());
    assertEquals(exportConfig.getExportTypeSpecificParameters(), job.getExportTypeSpecificParameters());*/
    validateJob(job, exportConfig);

    JobDetail jobDetail = scheduledJobDetails.jobDetail();
    assertNotNull(jobDetail);
    assertEquals(JOB_ID, jobDetail.getKey().getName());
    //assertEquals(JOB_STRING_VALUE, jobDetail.getJobDataMap().get("job"));
    assertEquals(TENANT, jobDetail.getJobDataMap().get("tenantId"));
    assertEquals(EXPORT_CONFIG_ID, jobDetail.getJobDataMap().get("exportConfigId"));
  }

  /*@Test
  void testConvertExportConfigWithNullScheduleIdToJobDetailSuccessful() {
    ExportConfig exportConfig = buildExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig().getEdiSchedule()
      .getScheduleParameters().setId(null);
    ScheduledJobDetails scheduledJobDetails = converter.convert(exportConfig, JOB_ID);

    Job job = scheduledJobDetails.job();
    validateJob(job, exportConfig);

    JobDetail jobDetail = scheduledJobDetails.jobDetail();
    assertNotNull(jobDetail);
    //key should be set to exportConfig id in case of schedule id absence
    assertEquals(EXPORT_CONFIG_ID, jobDetail.getKey().getName());
  }*/


  private ExportConfig buildExportConfig() {
    //ExportTypeSpecificParameters params = new ExportTypeSpecificParameters().vendorEdiOrdersExportConfig(new VendorEdiOrdersExportConfig().)
    ExportConfig exportConfig = new ExportConfig()
      .id(EXPORT_CONFIG_ID)
      .type(EXPORT_TYPE)
      .tenant(TENANT)
      .exportTypeSpecificParameters(new ExportTypeSpecificParameters()
        .vendorEdiOrdersExportConfig(new VendorEdiOrdersExportConfig()
          .ediSchedule(new EdiSchedule()
            .scheduleParameters(new ScheduleParameters().id(UUID.fromString(SCHEDULE_ID))))));
    return exportConfig;
  }

  private void validateJob(Job job, ExportConfig exportConfig) {
    assertNotNull(job);
    assertNull(job.getId());
    assertEquals(EXPORT_TYPE, job.getType());
    assertEquals(TENANT, job.getTenant());
    assertEquals(true, job.getIsSystemSource());
    assertEquals(exportConfig.getExportTypeSpecificParameters(), job.getExportTypeSpecificParameters());
  }
}
