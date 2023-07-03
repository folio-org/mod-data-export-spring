package org.folio.des.scheduling.quartz.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.service.JobService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.impl.JobDetailImpl;

@ExtendWith(MockitoExtension.class)
class OldDeleteJobTest {

  private static final String TENANT_ID = "some_test_tenant";
  private static final String EXPORT_CONFIG_ID = "some_test_export_config_id";
  private static final JobKey JOB_KEY = JobKey.jobKey("testJobKey", "testJobGroup");
  @Mock
  private JobService jobService;
  @Mock
  private FolioExecutionContextHelper contextHelper;
  @InjectMocks
  private OldDeleteJob oldDeleteJob;
  @Mock
  private JobExecutionContext jobExecutionContext;
  private final FolioExecutionContext folioExecutionContext = new TestFolioExecutionContext();

  @Test
  void testSuccessfulExecute() throws JobExecutionException {
    when(jobExecutionContext.getJobDetail()).thenReturn(getJobDetail());
    when(contextHelper.getFolioExecutionContext(any())).thenReturn(folioExecutionContext);
    doNothing().when(jobService).deleteOldJobs();
    oldDeleteJob.execute(jobExecutionContext);
    verify(contextHelper).getFolioExecutionContext(TENANT_ID);
  }

  @Test
  void testExecuteFailureWhenNoTenantIdPassed() {
    JobDetail jobDetail = getJobDetail();
    jobDetail.getJobDataMap().remove("tenantId");
    when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);

    String message = assertThrows(IllegalArgumentException.class, () -> oldDeleteJob.execute(jobExecutionContext)).getMessage();
    assertEquals("'tenantId' param is missing in the jobExecutionContext", message);
  }

  private JobDetail getJobDetail() {
    var jobDetail = new JobDetailImpl();
    var jobDataMap = jobDetail.getJobDataMap();
    jobDataMap.put("tenantId", TENANT_ID);
    jobDataMap.put("exportConfigId", EXPORT_CONFIG_ID);
    jobDetail.setKey(JOB_KEY);
    return jobDetail;
  }

  private static class TestFolioExecutionContext implements FolioExecutionContext {
    @Override
    public FolioModuleMetadata getFolioModuleMetadata() {
      return new FolioModuleMetadata() {
        @Override
        public String getModuleName() {
          return "moduleName";
        }

        @Override
        public String getDBSchemaName(String tenantId) {
          return "dbSchemaName";
        }
      };
    }
  }
}
