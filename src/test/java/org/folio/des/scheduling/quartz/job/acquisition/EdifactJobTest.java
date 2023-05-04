package org.folio.des.scheduling.quartz.job.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.des.builder.job.JobCommandSchedulerBuilder;
import org.folio.des.config.FolioExecutionContextHelper;
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
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;

@ExtendWith(MockitoExtension.class)
class EdifactJobTest {
  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private JobService jobService;
  @Mock
  private JobCommandSchedulerBuilder jobSchedulerCommandBuilder;
  @Mock
  private FolioExecutionContextHelper contextHelper;
  @Mock
  private ExportTypeBasedConfigManager exportTypeBasedConfigManager;
  @InjectMocks
  private EdifactJob edifactJob;
  @Mock
  private JobExecutionContext jobExecutionContext;
  @Mock
  private Scheduler scheduler;
  private FolioExecutionContext folioExecutionContext = new TestFolioExecutionContext();
  private static final String TENANT_ID = "some_test_tenant";
  private static final String EXPORT_CONFIG_ID = "some_test_export_config_id";
  private static final ExportType EXPORT_TYPE = ExportType.EDIFACT_ORDERS_EXPORT;
  private static final JobKey JOB_KEY = JobKey.jobKey("testJobKey", "testJobGroup");

  @Test
  void testExecuteSuccessful() {
    when(jobExecutionContext.getJobDetail()).thenReturn(getJobDetail());
    when(exportTypeBasedConfigManager.getConfigById(EXPORT_CONFIG_ID)).thenReturn(getExportConfig());
    when(contextHelper.getFolioExecutionContext(any())).thenReturn(folioExecutionContext);
    when(jobService.upsertAndSendToKafka(any(), eq(false), eq(false))).thenReturn(new Job().id(UUID.randomUUID()));

    edifactJob.execute(jobExecutionContext);

    verify(contextHelper).getFolioExecutionContext(TENANT_ID);
    verify(jobService).upsertAndSendToKafka(any(), eq(false), eq(false));
    verify(jobSchedulerCommandBuilder).buildJobCommand(any());
    verify(jobExecutionService).sendJobCommand(any());
  }

  @Test
  void testExecuteSuccessfulSkipKafkaWhenNoJobId() {
    when(jobExecutionContext.getJobDetail()).thenReturn(getJobDetail());
    when(exportTypeBasedConfigManager.getConfigById(EXPORT_CONFIG_ID)).thenReturn(getExportConfig());
    when(contextHelper.getFolioExecutionContext(any())).thenReturn(folioExecutionContext);
    when(jobService.upsertAndSendToKafka(any(), eq(false), eq(false))).thenReturn(new Job());

    edifactJob.execute(jobExecutionContext);

    verify(contextHelper).getFolioExecutionContext(TENANT_ID);
    verify(jobService).upsertAndSendToKafka(any(), eq(false), eq(false));
    verify(jobSchedulerCommandBuilder, times(0)).buildJobCommand(any());
    verify(jobExecutionService, times(0)).sendJobCommand(any());
  }

  @Test
  void testExecuteFailureWhenNoTenantIdPassed() {
    JobDetail jobDetail = getJobDetail();
    jobDetail.getJobDataMap().remove("tenantId");
    when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);

    verifyExceptionThrownAndJobNotExecuted(IllegalArgumentException.class,
      "'tenantId' param is missing in the jobExecutionContext", jobExecutionContext);
  }

  @Test
  void testExecuteFailureWhenNoExportConfigIdPassed() {
    JobDetail jobDetail = getJobDetail();
    jobDetail.getJobDataMap().remove("exportConfigId");
    when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
    when(contextHelper.getFolioExecutionContext(any())).thenReturn(folioExecutionContext);

    verifyExceptionThrownAndJobNotExecuted(IllegalArgumentException.class,
      "'exportConfigId' param is missing in the jobExecutionContext", jobExecutionContext);
  }

  @Test
  void testExecuteFailureAndJobDeletedWhenExportConfigNotFound() throws SchedulerException {
    when(jobExecutionContext.getJobDetail()).thenReturn(getJobDetail());
    when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
    when(contextHelper.getFolioExecutionContext(any())).thenReturn(folioExecutionContext);
    when(exportTypeBasedConfigManager.getConfigById(EXPORT_CONFIG_ID))
      .thenThrow(new NotFoundException("config not found"));

    verifyExceptionThrownAndJobNotExecuted(SchedulingException.class,
      "Configuration id 'some_test_export_config_id' could not be found. The job was unscheduled",
      jobExecutionContext);
    verify(scheduler).deleteJob(JOB_KEY);
  }

  private void verifyExceptionThrownAndJobNotExecuted(Class exceptionClass, String exceptionMessage,
                                                      JobExecutionContext jobExecutionContext) {
    Throwable ex = assertThrows(exceptionClass, () -> edifactJob.execute(jobExecutionContext));
    assertEquals(exceptionMessage, ex.getMessage());
    verify(jobService, times(0)).upsertAndSendToKafka(any(), anyBoolean());
    verify(jobSchedulerCommandBuilder, times(0)).buildJobCommand(any());
    verify(jobExecutionService, times(0)).sendJobCommand(any());
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
