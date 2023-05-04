package org.folio.des.scheduling.quartz.converter.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;

class ExportConfigToEdifactJobDetailConverterTest {
  private static final String EXPORT_CONFIG_ID = "3962583c-7b88-4000-be7d-5e1fe58a3416";
  private static final String SCHEDULE_ID = "8d9626bb-b507-4aeb-a115-8b3d26bbc221";
  private static final ExportType EXPORT_TYPE = ExportType.EDIFACT_ORDERS_EXPORT;
  private static final String TENANT = "some_test_tenant";
  private static final String JOB_ID = UUID.randomUUID().toString();
  ExportConfigToEdifactJobDetailConverter converter = new ExportConfigToEdifactJobDetailConverter();

  @Test
  void testConvertNullReturnsNull() {
    assertNull(converter.convert(null, null));
  }

  @Test
  void testConvertExportConfigToJobDetailSuccessful() {
    ExportConfig exportConfig = buildExportConfig();
    var jobDetail = converter.convert(exportConfig, JobKey.jobKey(JOB_ID));

    assertNotNull(jobDetail);
    assertEquals(JOB_ID, jobDetail.getKey().getName());
    assertEquals(TENANT, jobDetail.getJobDataMap().get("tenantId"));
    assertEquals(EXPORT_CONFIG_ID, jobDetail.getJobDataMap().get("exportConfigId"));
  }

  private ExportConfig buildExportConfig() {
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
}
