package org.folio.des.builder.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.de.entity.JobCommand;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.IdentifierType;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.Progress;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SpringBootTest(classes = { EdifactOrdersJobCommandSchedulerBuilderTest.MockSpringContext.class})
class EdifactOrdersJobCommandSchedulerBuilderTest {
  @Autowired
  private EdifactOrdersJobCommandSchedulerBuilder builder;
  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void successJobCommandBuild() {
    Job job = new Job();
    UUID id = UUID.randomUUID();
    String expId = UUID.randomUUID().toString();
    UUID vendorId = UUID.randomUUID();
    job.setId(id);
    job.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    job.setIdentifierType(IdentifierType.ID);
    job.setEntityType(EntityType.USER);
    job.setProgress(new Progress());
    job.setName("Test job");
    job.setDescription("Description");
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setId(expId);
    ediConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setVendorId(vendorId);
    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    ediConfig.exportTypeSpecificParameters(parameters);
    job.setExportTypeSpecificParameters(parameters);
    JobCommand actJobCommand = builder.buildJobCommand(job);

    JobParameter actJobParameter = actJobCommand.getJobParameters().getParameters().get("edifactOrdersExport");
    assertEquals(id, actJobCommand.getId());
    assertTrue(actJobParameter.getValue().toString().contains(vendorId.toString()));
  }

  public static class MockSpringContext {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
      OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules()
        .registerModule(new SimpleModule().addDeserializer(
          ExitStatus.class, new EdifactOrdersJobCommandSchedulerBuilderTest.MockSpringContext.ExitStatusDeserializer()))
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

      @Override public ExitStatus deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return EXIT_STATUSES.get(((JsonNode) jp.getCodec().readTree(jp)).get("exitCode").asText());
      }

    }

    @Bean public ObjectMapper objectMapper() {
      return OBJECT_MAPPER;
    }

    @Bean EdifactOrdersJobCommandSchedulerBuilder builder(ObjectMapper objectMapper) {
      return new EdifactOrdersJobCommandSchedulerBuilder(objectMapper);
    }
  }
}
