package org.folio.des.builder.scheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.*;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.service.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(classes = { EdifactScheduledTaskBuilderTest.MockSpringContext.class})
class EdifactScheduledTaskBuilderTest {
  @Autowired
  private EdifactScheduledTaskBuilder builder;
  @Autowired
  private FolioExecutionContextHelper contextHelperMock;
  @Autowired
  private JobService jobServiceMock;
  @Autowired
  private AcqSchedulingProperties acqSchedulingProperties;

  @AfterEach
  void afterEach() {
    Mockito.reset(contextHelperMock, jobServiceMock);
  }

  @Test
  void shouldCreateTaskIfExportConfigIsProvidedWithoutSpecificParametersAndModuleRegistered()
    throws ExecutionException, InterruptedException {
    String expId = UUID.randomUUID().toString();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);

    doReturn(true).when(contextHelperMock).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();

    Job scheduledJob = new Job();
    scheduledJob.setType(ediConfig.getType());
    scheduledJob.setIsSystemSource(true);

    Mockito.when(jobServiceMock.upsert(any())).thenReturn(scheduledJob);

    Optional<ScheduledTask> scheduledTask = builder.buildTask(ediConfig);

    assertNotNull(scheduledTask.get().getJob());
    ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> actJobFuture = service.submit(scheduledTask.get().getTask());

    Object actJob = actJobFuture.get();
    service.shutdown();
    verify(jobServiceMock, times(2)).upsert(any());
    verify(contextHelperMock, never()).initScope();
  }

  @Test
  void shouldCreateTaskIfExportConfigIsProvidedWithSpecificParametersAndModuleRegistered()
    throws ExecutionException, InterruptedException {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    doReturn(true).when(contextHelperMock).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();

    Job scheduledJob = new Job();
    scheduledJob.setType(ediConfig.getType());
    scheduledJob.setIsSystemSource(true);

    Mockito.when(jobServiceMock.upsert(any())).thenReturn(scheduledJob);

    Optional<ScheduledTask> scheduledTask = builder.buildTask(ediConfig);
    assertNotNull(scheduledTask.get().getJob());

    ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> actJobFuture = service.submit(scheduledTask.get().getTask());

    Object actJob = actJobFuture.get();
    service.shutdown();
    verify(jobServiceMock, times(2)).upsert(any());
    verify(contextHelperMock, never()).initScope();
  }

  @Test
  void shouldBeEmpty() {
    Optional<Job> jobs = builder.createScheduledJob(null);
    assertTrue(jobs.isEmpty());
  }

  public static class MockSpringContext {
    @Bean
    @Primary
    FolioExecutionContextHelper contextHelperMock() {
      return mock(FolioExecutionContextHelper.class);
    }
    @Bean
    JobService jobServiceMock() {
      return mock(JobService.class);
    }
    @Bean AcqSchedulingProperties acqSchedulingProperties() {
      return mock(AcqSchedulingProperties.class);
    }
    @Bean EdifactScheduledTaskBuilder builder(JobService jobServiceMock, FolioExecutionContextHelper contextHelperMock,
                                              AcqSchedulingProperties acqSchedulingProperties) {
      return new EdifactScheduledTaskBuilder(jobServiceMock, contextHelperMock, acqSchedulingProperties);
    }
  }
}
