package org.folio.des.builder.scheduling;

import static org.folio.des.support.BaseTest.TENANT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.folio.de.entity.JobCommand;
import org.folio.des.builder.job.EdifactOrdersJobCommandSchedulerBuilder;
import org.folio.des.builder.job.JobCommandSchedulerBuilder;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SpringBootTest(classes = {EdifactScheduledTaskBuilderTest.MockSpringContext.class})
class EdifactScheduledTaskBuilderTest {
  @Autowired
  private EdifactScheduledTaskBuilder builder;
  @Autowired
  private FolioExecutionContextHelper contextHelperMock;
  @Autowired
  private JobService jobServiceMock;
  @Autowired
  private AcqSchedulingProperties acqSchedulingProperties;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private EdifactOrdersJobCommandSchedulerBuilder edifactOrdersJobCommandSchedulerBuilder;
  @Autowired
  private JobExecutionService jobExecutionService;

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
    ediConfig.setTenant(TENANT);

    doReturn(true).when(contextHelperMock).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();

    Job scheduledJob = new Job();
    scheduledJob.setType(ediConfig.getType());
    scheduledJob.setIsSystemSource(true);

    Mockito.when(jobServiceMock.upsertAndSendToKafka(any(), eq(false))).thenReturn(scheduledJob);

    Optional<ScheduledTask> scheduledTask = builder.buildTask(ediConfig);

    assertNotNull(scheduledTask.get().getJob());
    ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> actJobFuture = service.submit(scheduledTask.get().getTask());

    Object actJob = actJobFuture.get();
    service.shutdown();
    verify(jobServiceMock).upsertAndSendToKafka(any(), eq(false));
    verify(contextHelperMock).initScope(TENANT);
    verify(contextHelperMock).finishContext();
  }

  @Test
  void shouldCreateTaskIfExportConfigIsProvidedWithSpecificParametersAndModuleRegisteredAndJobIdNull()
    throws ExecutionException, InterruptedException {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ediConfig.setTenant(TENANT);
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

    Mockito.when(jobServiceMock.upsertAndSendToKafka(any(), eq(false))).thenReturn(scheduledJob);

    Optional<ScheduledTask> scheduledTask = builder.buildTask(ediConfig);
    assertNotNull(scheduledTask.get().getJob());

    ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> actJobFuture = service.submit(scheduledTask.get().getTask());

    Object actJob = actJobFuture.get();
    service.shutdown();
    verify(jobServiceMock).upsertAndSendToKafka(any(), eq(false));
    verify(contextHelperMock).initScope(TENANT);
    verify(contextHelperMock).finishContext();
    verify(edifactOrdersJobCommandSchedulerBuilder, times(0)).buildJobCommand(scheduledJob);
    verify(jobExecutionService, times(0)).sendJobCommand(any());
  }

  @Test
  void shouldCreateTaskIfExportConfigIsProvidedWithSpecificParametersAndModuleRegisteredAndJobIdExist()
    throws ExecutionException, InterruptedException {
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ediConfig.setTenant(TENANT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);

    doReturn(true).when(contextHelperMock).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();

    Job scheduledJob = new Job();
    scheduledJob.setId(UUID.randomUUID());
    scheduledJob.setType(ediConfig.getType());
    scheduledJob.setIsSystemSource(true);

    JobCommand jobCommand = new JobCommand();
    jobCommand.setId(UUID.randomUUID());
    Mockito.when(jobServiceMock.upsertAndSendToKafka(any(), eq(false))).thenReturn(scheduledJob);
    Mockito.when(edifactOrdersJobCommandSchedulerBuilder.buildJobCommand(scheduledJob)).thenReturn(jobCommand);
    Mockito.doNothing().when(jobExecutionService).sendJobCommand(any());

    Optional<ScheduledTask> scheduledTask = builder.buildTask(ediConfig);
    assertNotNull(scheduledTask.get().getJob());

    ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> actJobFuture = service.submit(scheduledTask.get().getTask());

    Object actJob = actJobFuture.get();
    service.shutdown();
    verify(jobServiceMock).upsertAndSendToKafka(any(), eq(false));
    verify(contextHelperMock).initScope(TENANT);
    verify(contextHelperMock).finishContext();
    verify(edifactOrdersJobCommandSchedulerBuilder, times(1)).buildJobCommand(any(Job.class));
    verify(jobExecutionService, times(1)).sendJobCommand(any());
  }

  @Test
  void shouldBeEmpty() {
    Optional<Job> jobs = builder.createScheduledJob(null);
    assertTrue(jobs.isEmpty());
  }

  public static class MockSpringContext {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
      OBJECT_MAPPER =
        new ObjectMapper()
          .findAndRegisterModules()
          .registerModule(
            new SimpleModule()
              .addDeserializer(ExitStatus.class, new MockSpringContext.ExitStatusDeserializer())
              .addDeserializer(JobParameter.class, new MockSpringContext.JobParameterDeserializer()))
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    static class ExitStatusDeserializer extends StdDeserializer<ExitStatus> {

      private static final Map<String, ExitStatus> EXIT_STATUSES = new HashMap<>();

      static {
        EXIT_STATUSES.put("UNKNOWN", ExitStatus.UNKNOWN);
        EXIT_STATUSES.put("EXECUTING", ExitStatus.EXECUTING);
        EXIT_STATUSES.put("COMPLETED", ExitStatus.COMPLETED);
        EXIT_STATUSES.put("NOOP", ExitStatus.NOOP);
        EXIT_STATUSES.put("FAILED", ExitStatus.FAILED);
        EXIT_STATUSES.put("STOPPED", ExitStatus.STOPPED);
      }

      public ExitStatusDeserializer() {
        this(null);
      }

      public ExitStatusDeserializer(Class<?> vc) {
        super(vc);
      }

      @Override
      public ExitStatus deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return EXIT_STATUSES.get(((JsonNode) jp.getCodec().readTree(jp)).get("exitCode").asText());
      }

    }

    static class JobParameterDeserializer extends StdDeserializer<JobParameter<?>> {

      private static final String VALUE_PARAMETER_PROPERTY = "value";

      public JobParameterDeserializer() {
        this(null);
      }

      public JobParameterDeserializer(Class<?> vc) {
        super(vc);
      }

      @Override
      public JobParameter<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = jp.getCodec().readTree(jp);
        var identifying = jsonNode.get("identifying").asBoolean();
        switch (jsonNode.get("type").asText()) {
          case "STRING" -> new JobParameter<>(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText(), String.class, identifying);
          case "DATE" -> new JobParameter<>(
            Date.valueOf(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText()), Date.class, identifying);
          case "LONG" -> new JobParameter<>(jsonNode.get(VALUE_PARAMETER_PROPERTY).asLong(), Long.class, identifying);
          case "DOUBLE" -> new JobParameter<>(jsonNode.get(VALUE_PARAMETER_PROPERTY).asDouble(), Double.class, identifying);
        }
        return null;
      }
    }

    @Bean
    public ObjectMapper objectMapper() {
      return OBJECT_MAPPER;
    }

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
    @Bean JobExecutionService jobExecutionService() {
      return mock(JobExecutionService.class);
    }
    @Bean EdifactOrdersJobCommandSchedulerBuilder edifactOrdersJobCommandSchedulerBuilder() {
      return mock(EdifactOrdersJobCommandSchedulerBuilder.class);
    }

    @Bean EdifactScheduledTaskBuilder builder(JobService jobServiceMock, FolioExecutionContextHelper contextHelperMock,
                                              AcqSchedulingProperties acqSchedulingProperties,
                                              JobExecutionService jobExecutionService,
                                              JobCommandSchedulerBuilder edifactOrdersJobCommandSchedulerBuilder) {
      return new EdifactScheduledTaskBuilder(jobServiceMock, contextHelperMock, acqSchedulingProperties,
                    jobExecutionService, edifactOrdersJobCommandSchedulerBuilder);
    }
  }
}
