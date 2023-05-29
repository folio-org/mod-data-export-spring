package org.folio.des.scheduling.quartz.job.bursar;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;

import static org.folio.des.scheduling.quartz.QuartzConstants.BURSAR_EXPORT_GROUP_NAME;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BursarJobKeyResolverTest {

  private static final String EXPORT_CONFIG_ID = "3962583c-7b88-4000-be7d-5e1fe58a3416";
  private static final ExportType EXPORT_TYPE = ExportType.BURSAR_FEES_FINES;
  private static final String TENANT = "diku";

  private static final String EXPORT_GROUP = TENANT + "_" + BURSAR_EXPORT_GROUP_NAME;
  private final BursarJobKeyResolver bursarJobKeyResolver = new BursarJobKeyResolver();

  @Test
  void testResolveJobJey() {
    JobKey jobKey = bursarJobKeyResolver.resolve(buildExportConfig());
    assertNotNull(jobKey);
    assertEquals(JobKey.jobKey(EXPORT_CONFIG_ID, EXPORT_GROUP),jobKey);
  }

  @Test
  void testNullResolveJobJey() {
    JobKey jobKey = bursarJobKeyResolver.resolve(null);
    assertNull(jobKey);
  }


  private ExportConfig buildExportConfig() {
    ExportConfig exportConfig = new ExportConfig()
      .id(EXPORT_CONFIG_ID)
      .type(EXPORT_TYPE)
      .tenant(TENANT);
    return exportConfig;
  }

}
