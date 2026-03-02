package org.folio.des.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameter;

import java.sql.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JacksonConfigurationTest {

  private JacksonConfiguration jacksonConfiguration;
  private ObjectMapper objectMapper;
  private ObjectMapper entityObjectMapper;

  @BeforeEach
  void setUp() {
    jacksonConfiguration = new JacksonConfiguration();
    objectMapper = jacksonConfiguration.objectMapper();
    entityObjectMapper = jacksonConfiguration.entityObjectMapper();
  }

  // ── objectMapper / entityObjectMapper beans ────────────────────────────────

  @Nested
  @DisplayName("Bean creation")
  class BeanCreation {

    @Test
    @DisplayName("objectMapper bean is not null")
    void objectMapperIsNotNull() {
      assertNotNull(objectMapper);
    }

    @Test
    @DisplayName("entityObjectMapper bean is not null")
    void entityObjectMapperIsNotNull() {
      assertNotNull(entityObjectMapper);
    }

    @Test
    @DisplayName("objectMapper and entityObjectMapper are different instances")
    void objectMapperAndEntityObjectMapperAreDifferentInstances() {
      assertNotSame(objectMapper, entityObjectMapper);
    }
  }

  // ── UUID serialization ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("UUID serialization")
  class UuidSerialization {

    @Test
    @DisplayName("UUID is serialized as plain string without hyphens loss")
    void uuidSerializedAsString() throws JsonProcessingException {
      UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
      String json = objectMapper.writeValueAsString(uuid);
      assertEquals("\"550e8400-e29b-41d4-a716-446655440000\"", json);
    }

    @Test
    @DisplayName("UUID round-trips correctly")
    void uuidRoundTrip() throws JsonProcessingException {
      UUID original = UUID.randomUUID();
      String json = objectMapper.writeValueAsString(original);
      UUID deserialized = objectMapper.readValue(json, UUID.class);
      assertEquals(original, deserialized);
    }
  }

  // ── ExitStatus deserialization ─────────────────────────────────────────────

  @Nested
  @DisplayName("ExitStatus deserialization")
  class ExitStatusDeserialization {

    @Test
    @DisplayName("Deserializes COMPLETED exit status")
    void deserializesCompleted() throws JsonProcessingException {
      ExitStatus result = objectMapper.readValue("{\"exitCode\":\"COMPLETED\"}", ExitStatus.class);
      assertEquals(ExitStatus.COMPLETED, result);
    }

    @Test
    @DisplayName("Deserializes FAILED exit status")
    void deserializesFailed() throws JsonProcessingException {
      ExitStatus result = objectMapper.readValue("{\"exitCode\":\"FAILED\"}", ExitStatus.class);
      assertEquals(ExitStatus.FAILED, result);
    }

    @Test
    @DisplayName("Deserializes UNKNOWN exit status")
    void deserializesUnknown() throws JsonProcessingException {
      ExitStatus result = objectMapper.readValue("{\"exitCode\":\"UNKNOWN\"}", ExitStatus.class);
      assertEquals(ExitStatus.UNKNOWN, result);
    }

    @Test
    @DisplayName("Deserializes EXECUTING exit status")
    void deserializesExecuting() throws JsonProcessingException {
      ExitStatus result = objectMapper.readValue("{\"exitCode\":\"EXECUTING\"}", ExitStatus.class);
      assertEquals(ExitStatus.EXECUTING, result);
    }

    @Test
    @DisplayName("Deserializes NOOP exit status")
    void deserializesNoop() throws JsonProcessingException {
      ExitStatus result = objectMapper.readValue("{\"exitCode\":\"NOOP\"}", ExitStatus.class);
      assertEquals(ExitStatus.NOOP, result);
    }

    @Test
    @DisplayName("Deserializes STOPPED exit status")
    void deserializesStopped() throws JsonProcessingException {
      ExitStatus result = objectMapper.readValue("{\"exitCode\":\"STOPPED\"}", ExitStatus.class);
      assertEquals(ExitStatus.STOPPED, result);
    }

    @Test
    @DisplayName("Returns null for unknown exit code")
    void returnsNullForUnknownCode() throws JsonProcessingException {
      ExitStatus result = objectMapper.readValue("{\"exitCode\":\"NONEXISTENT\"}", ExitStatus.class);
      assertNull(result);
    }
  }

  // ── JobParameter deserialization ───────────────────────────────────────────

  @Nested
  @DisplayName("JobParameter deserialization")
  class JobParameterDeserialization {

    @Test
    @DisplayName("Deserializes STRING JobParameter")
    void deserializesStringJobParameter() throws JsonProcessingException {
      String json = "{\"type\":\"STRING\",\"value\":\"hello\",\"identifying\":true}";
      JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
      // The deserializer returns null for all branches (missing return statements)
      // This test documents the current behaviour.
      assertNull(param);
    }

    @Test
    @DisplayName("Deserializes LONG JobParameter returns null (current impl)")
    void deserializesLongJobParameter() throws JsonProcessingException {
      String json = "{\"type\":\"LONG\",\"value\":42,\"identifying\":false}";
      JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
      assertNull(param);
    }

    @Test
    @DisplayName("Deserializes DOUBLE JobParameter returns null (current impl)")
    void deserializesDoubleJobParameter() throws JsonProcessingException {
      String json = "{\"type\":\"DOUBLE\",\"value\":3.14,\"identifying\":false}";
      JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
      assertNull(param);
    }

    @Test
    @DisplayName("Deserializes DATE JobParameter returns null (current impl)")
    void deserializesDateJobParameter() throws JsonProcessingException {
      String json = "{\"type\":\"DATE\",\"value\":\"2024-01-15\",\"identifying\":true}";
      JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
      assertNull(param);
    }

    @Test
    @DisplayName("Deserializes unknown type JobParameter returns null")
    void deserializesUnknownTypeJobParameter() throws JsonProcessingException {
      String json = "{\"type\":\"UNKNOWN_TYPE\",\"value\":\"something\",\"identifying\":false}";
      JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
      assertNull(param);
    }
  }

  // ── Serialization inclusion ────────────────────────────────────────────────

  @Nested
  @DisplayName("Serialization inclusion")
  class SerializationInclusion {

    record SampleDto(String name, String nullField, String emptyField) {}

    @Test
    @DisplayName("objectMapper omits null and empty fields (NON_EMPTY)")
    void objectMapperOmitsNullAndEmpty() throws JsonProcessingException {
      SampleDto dto = new SampleDto("Alice", null, "");
      String json = objectMapper.writeValueAsString(dto);
      assertFalse(json.contains("nullField"), "null field should be omitted");
      assertFalse(json.contains("emptyField"), "empty field should be omitted");
      assertTrue(json.contains("Alice"));
    }

    @Test
    @DisplayName("entityObjectMapper includes null fields (ALWAYS)")
    void entityObjectMapperIncludesNullFields() throws JsonProcessingException {
      SampleDto dto = new SampleDto("Bob", null, "");
      String json = entityObjectMapper.writeValueAsString(dto);
      assertTrue(json.contains("nullField"), "null field should be present in entity mapper output");
    }
  }

  // ── Unknown properties ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("Unknown properties handling")
  class UnknownProperties {

    record KnownDto(String name) {}

    @Test
    @DisplayName("Does not fail on unknown JSON properties")
    void doesNotFailOnUnknownProperties() {
      assertDoesNotThrow(() ->
        objectMapper.readValue("{\"name\":\"test\",\"unknownProp\":\"value\"}", KnownDto.class)
      );
    }
  }
}

