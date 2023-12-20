package org.folio.des.scheduling.quartz.job.bursar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.des.builder.job.JobCommandSchedulerBuilder;
import org.folio.des.client.DataExportSpringClient;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.exceptions.SchedulingException;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.model.SystemUser;
import org.folio.spring.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;

@ExtendWith(MockitoExtension.class)
class BursarJobTest {

  private static final String TENANT_ID = "some_test_tenant";
  private static final String EXPORT_CONFIG_ID = "some_test_export_config_id";
  private static final JobKey JOB_KEY = JobKey.jobKey("testJobKey", "testJobGroup");
  private static final ExportType EXPORT_TYPE = ExportType.BURSAR_FEES_FINES;

  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private JobService jobService;
  @Mock
  private JobCommandSchedulerBuilder jobSchedulerCommandBuilder;
  @Mock
  private ExecutionContextBuilder contextBuilder;
  @Mock
  private SystemUserService systemUserService;
  @Mock
  private ExportTypeBasedConfigManager exportTypeBasedConfigManager;
  @InjectMocks
  private BursarJob bursarJob;
  @Mock
  private JobExecutionContext jobExecutionContext;
  @Mock
  private Scheduler scheduler;
  @Mock
  private DataExportSpringClient dataExportSpringClient;
  private final FolioExecutionContext folioExecutionContext = new TestFolioExecutionContext();

  @Test
  void testSuccessfulExecute() throws JobExecutionException {
    when(jobExecutionContext.getJobDetail()).thenReturn(getJobDetail());
    when(exportTypeBasedConfigManager.getConfigById(EXPORT_CONFIG_ID)).thenReturn(getExportConfig());
    when(systemUserService.getAuthedSystemUser(any())).thenReturn(SystemUser.builder().build());
    when(contextBuilder.forSystemUser(any())).thenReturn(folioExecutionContext);
    when(dataExportSpringClient.upsertJob(any())).thenReturn(new Job().id(UUID.randomUUID()));
    bursarJob.execute(jobExecutionContext);
    verify(dataExportSpringClient).upsertJob(any());
  }

  @Test
  void testExecuteFailureWhenNoTenantIdPassed() {
    JobDetail jobDetail = getJobDetail();
    jobDetail.getJobDataMap().remove("tenantId");
    when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);

    String message = assertThrows(IllegalArgumentException.class, () -> bursarJob.execute(jobExecutionContext)).getMessage();
    assertEquals("'tenantId' param is missing in the jobExecutionContext", message);
  }

  @Test
  void testExecuteFailureWhenConfigIdNotFoundInJobDetail() {
    JobDetail jobDetail = getJobDetail();
    jobDetail.getJobDataMap().remove("exportConfigId");
    when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
    when(systemUserService.getAuthedSystemUser(any())).thenReturn(SystemUser.builder().build());
    when(contextBuilder.forSystemUser(any())).thenReturn(folioExecutionContext);

    String message = assertThrows(IllegalArgumentException.class, () -> bursarJob.execute(jobExecutionContext)).getMessage();
    assertEquals("'exportConfigId' param is missing in the jobExecutionContext", message);
  }

  @Test
  void testExecuteFailureWhenWhenConfigIdNotFoundInSettings() throws SchedulerException {
    when(jobExecutionContext.getJobDetail()).thenReturn(getJobDetail());
    when(systemUserService.getAuthedSystemUser(any())).thenReturn(SystemUser.builder().build());
    when(contextBuilder.forSystemUser(any())).thenReturn(folioExecutionContext);
    when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
    when(exportTypeBasedConfigManager.getConfigById(EXPORT_CONFIG_ID))
      .thenThrow(new NotFoundException("config not found"));
    String message = assertThrows(SchedulingException.class, () -> bursarJob.execute(jobExecutionContext)).getMessage();
    assertEquals("Configuration id 'some_test_export_config_id' could not be found. The job was unscheduled", message);
    verify(scheduler).deleteJob(JOB_KEY);
  }

  private JobDetail getJobDetail() {
    var jobDetail = new JobDetailImpl();
    var jobDataMap = jobDetail.getJobDataMap();
    jobDataMap.put("tenantId", TENANT_ID);
    jobDataMap.put("exportConfigId", EXPORT_CONFIG_ID);
    jobDetail.setKey(JOB_KEY);
    return jobDetail;
  }

  private ExportConfig getExportConfig() {
    ExportConfig exportConfig = new ExportConfig()
      .id(EXPORT_CONFIG_ID)
      .type(EXPORT_TYPE)
      .tenant(TENANT_ID)
      .exportTypeSpecificParameters(new ExportTypeSpecificParameters()
        .vendorEdiOrdersExportConfig(new VendorEdiOrdersExportConfig()
          .ediSchedule(new EdiSchedule()
            .scheduleParameters(new ScheduleParameters().id(UUID.randomUUID())))));
    return exportConfig;
  }

  private static class TestFolioExecutionContext implements org.folio.spring.FolioExecutionContext {
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
