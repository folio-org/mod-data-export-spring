package org.folio.des.builder.scheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.*;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.folio.des.service.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(classes = { BaseScheduledTaskBuilderTest.MockSpringContext.class})
class BaseScheduledTaskBuilderTest {

  @Autowired
  private BaseScheduledTaskBuilder builder;
  @Autowired
  private FolioExecutionContextHelper contextHelperMock;
  @Autowired
  private JobService jobServiceMock;

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
    doNothing().when(contextHelperMock).initScope();

    Job scheduledJob = new Job();
    scheduledJob.setType(ediConfig.getType());
    scheduledJob.setIsSystemSource(true);

    Mockito.when(jobServiceMock.upsert(any(), eq(true))).thenReturn(scheduledJob);

    Optional<ScheduledTask> scheduledTask = builder.buildTask(ediConfig);

    assertNotNull(scheduledTask.get().getJob());
    ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> actJobFuture = service.submit(scheduledTask.get().getTask());

    Object actJob = actJobFuture.get();
    service.shutdown();
    verify(jobServiceMock).upsert(any(), eq(true));
    verify(contextHelperMock).initScope();
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
    doNothing().when(contextHelperMock).initScope();

    Job scheduledJob = new Job();
    scheduledJob.setType(ediConfig.getType());
    scheduledJob.setIsSystemSource(true);

    Mockito.when(jobServiceMock.upsert(any(), eq(true))).thenReturn(scheduledJob);

    Optional<ScheduledTask> scheduledTask = builder.buildTask(ediConfig);
    assertNotNull(scheduledTask.get().getJob());

    ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> actJobFuture = service.submit(scheduledTask.get().getTask());

    Object actJob = actJobFuture.get();
    service.shutdown();
    verify(jobServiceMock).upsert(any(), eq(true));
    verify(contextHelperMock).initScope();
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

    @Bean BaseScheduledTaskBuilder builder(JobService jobServiceMock, FolioExecutionContextHelper contextHelperMock) {
      return new BaseScheduledTaskBuilder(jobServiceMock, contextHelperMock);
    }
  }
}
