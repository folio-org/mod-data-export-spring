package org.folio.des.scheduling.quartz.job.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;

class EdifactJobKeyResolverTest {
  private static final String EXPORT_CONFIG_ID = "3962583c-7b88-4000-be7d-5e1fe58a3416";
  private static final String SCHEDULE_ID = "8d9626bb-b507-4aeb-a115-8b3d26bbc221";
  private static final ExportType EXPORT_TYPE = ExportType.EDIFACT_ORDERS_EXPORT;
  private static final String TENANT = "some_test_tenant";
  private static final String EDIFACT_ORDERS_EXPORT = "edifact_orders_export";
  private final EdifactJobKeyResolver edifactJobKeyResolver = new EdifactJobKeyResolver();

  @Test
  void testResolveReturnsKeyFromScheduleId() {
    JobKey jobKey = edifactJobKeyResolver.resolve(buildExportConfig());
    assertNotNull(jobKey);
    assertEquals(JobKey.jobKey(SCHEDULE_ID, EDIFACT_ORDERS_EXPORT), jobKey);
  }

  @Test
  void testResolveReturnsKeyFromExportConfigIdWhenScheduleIdNull() {
    ExportConfig exportConfig = buildExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig().getEdiSchedule()
      .getScheduleParameters().setId(null);

    JobKey jobKey = edifactJobKeyResolver.resolve(exportConfig);

    assertNotNull(jobKey);
    assertEquals(JobKey.jobKey(EXPORT_CONFIG_ID, EDIFACT_ORDERS_EXPORT), jobKey);
  }

  @Test
  void testResolveThrowsExceptionWhenScheduleIdAndExportIdNull() {
    ExportConfig exportConfig = buildExportConfig();
    exportConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig().getEdiSchedule()
      .getScheduleParameters().setId(null);
    exportConfig.setId(null);

    assertThrows(IllegalArgumentException.class, () -> edifactJobKeyResolver.resolve(exportConfig),
      "Export config does not contain schedule id or export config id");
  }

  @Test
  void testResolveForNullExportConfigReturnsNull() {
    assertNull(edifactJobKeyResolver.resolve(null));
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
