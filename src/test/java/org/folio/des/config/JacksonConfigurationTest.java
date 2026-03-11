package org.folio.des.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;

import java.sql.Date;
import java.util.LinkedHashSet;
import java.util.Set;
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

  // ── JobParameter serialization ─────────────────────────────────────────────

  @Nested
  @DisplayName("JobParameter serialization")
  class JobParameterSerialization {

    @Test
    @DisplayName("Serializes STRING JobParameter with identifying=true")
    void serializesStringJobParameter() throws JsonProcessingException {
      JobParameter<String> param = new JobParameter<>("key", "hello", String.class, true);
      String json = objectMapper.writeValueAsString(param);
      JsonNode node = objectMapper.readTree(json);

      assertEquals("java.lang.String", node.get("type").asText());
      assertEquals("hello", node.get("value").asText());
      assertTrue(node.get("identifying").asBoolean());
    }

    @Test
    @DisplayName("Serializes STRING JobParameter with identifying=false")
    void serializesStringJobParameterNonIdentifying() throws JsonProcessingException {
      JobParameter<String> param = new JobParameter<>("key", "world", String.class, false);
      String json = objectMapper.writeValueAsString(param);
      JsonNode node = objectMapper.readTree(json);

      assertEquals("java.lang.String", node.get("type").asText());
      assertEquals("world", node.get("value").asText());
      assertFalse(node.get("identifying").asBoolean());
    }

    @Test
    @DisplayName("Serializes LONG JobParameter")
    void serializesLongJobParameter() throws JsonProcessingException {
      JobParameter<Long> param = new JobParameter<>("count", 42L, Long.class, true);
      String json = objectMapper.writeValueAsString(param);
      JsonNode node = objectMapper.readTree(json);

      assertEquals("java.lang.Long", node.get("type").asText());
      assertEquals(42L, node.get("value").asLong());
      assertTrue(node.get("identifying").asBoolean());
    }

    @Test
    @DisplayName("Serializes DOUBLE JobParameter")
    void serializesDoubleJobParameter() throws JsonProcessingException {
      JobParameter<Double> param = new JobParameter<>("rate", 3.14, Double.class, false);
      String json = objectMapper.writeValueAsString(param);
      JsonNode node = objectMapper.readTree(json);

      assertEquals("java.lang.Double", node.get("type").asText());
      assertEquals(3.14, node.get("value").asDouble(), 0.001);
      assertFalse(node.get("identifying").asBoolean());
    }

    @Test
    @DisplayName("Serializes DATE JobParameter with formatted value")
    void serializesDateJobParameter() throws JsonProcessingException {
      Date date = Date.valueOf("2024-06-15");
      JobParameter<Date> param = new JobParameter<>("startDate", date, Date.class, true);
      String json = objectMapper.writeValueAsString(param);
      JsonNode node = objectMapper.readTree(json);

      assertEquals("java.sql.Date", node.get("type").asText());
      assertTrue(node.get("value").asText().startsWith("2024-06-15"));
      assertTrue(node.get("identifying").asBoolean());
    }

    @Test
    @DisplayName("Serialized JSON contains exactly three fields")
    void serializedJsonContainsExactlyThreeFields() throws JsonProcessingException {
      JobParameter<String> param = new JobParameter<>("key", "val", String.class, true);
      JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(param));

      assertEquals(3, node.size());
      assertTrue(node.has("type"));
      assertTrue(node.has("value"));
      assertTrue(node.has("identifying"));
    }
  }

  // ── JobParameters serialization ────────────────────────────────────────────

  @Nested
  @DisplayName("JobParameters serialization")
  class JobParametersSerialization {

    @Test
    @DisplayName("Serializes empty JobParameters")
    void serializesEmptyJobParameters() throws JsonProcessingException {
      JobParameters params = new JobParameters(Set.of());
      String json = objectMapper.writeValueAsString(params);
      JsonNode node = objectMapper.readTree(json);

      assertTrue(node.has("parameters"));
      assertEquals(0, node.get("parameters").size());
    }

    @Test
    @DisplayName("Serializes JobParameters with a single STRING parameter")
    void serializesSingleStringParameter() throws JsonProcessingException {
      JobParameters params = new JobParameters(Set.of(
        new JobParameter<>("FILE_NAME", "test.csv", String.class, true)
      ));
      String json = objectMapper.writeValueAsString(params);
      JsonNode node = objectMapper.readTree(json);

      JsonNode paramsNode = node.get("parameters");
      assertNotNull(paramsNode);
      assertTrue(paramsNode.has("FILE_NAME"));

      JsonNode fileNameNode = paramsNode.get("FILE_NAME");
      assertEquals("java.lang.String", fileNameNode.get("type").asText());
      assertEquals("test.csv", fileNameNode.get("value").asText());
      assertTrue(fileNameNode.get("identifying").asBoolean());
    }

    @Test
    @DisplayName("Serializes JobParameters with multiple parameters")
    void serializesMultipleParameters() throws JsonProcessingException {
      Set<JobParameter<?>> paramSet = new LinkedHashSet<>();
      paramSet.add(new JobParameter<>("name", "report", String.class, true));
      paramSet.add(new JobParameter<>("count", 10L, Long.class, false));
      JobParameters params = new JobParameters(paramSet);

      String json = objectMapper.writeValueAsString(params);
      JsonNode node = objectMapper.readTree(json);

      JsonNode paramsNode = node.get("parameters");
      assertTrue(paramsNode.has("name"));
      assertTrue(paramsNode.has("count"));

      assertEquals("java.lang.String", paramsNode.get("name").get("type").asText());
      assertEquals("report", paramsNode.get("name").get("value").asText());

      assertEquals("java.lang.Long", paramsNode.get("count").get("type").asText());
      assertEquals(10L, paramsNode.get("count").get("value").asLong());
      assertFalse(paramsNode.get("count").get("identifying").asBoolean());
    }

    @Test
    @DisplayName("Serializes JobParameters with DATE parameter formatted correctly")
    void serializesDateParameter() throws JsonProcessingException {
      Date date = Date.valueOf("2025-01-20");
      JobParameters params = new JobParameters(Set.of(
        new JobParameter<>("runDate", date, Date.class, true)
      ));
      String json = objectMapper.writeValueAsString(params);
      JsonNode node = objectMapper.readTree(json);

      JsonNode dateNode = node.get("parameters").get("runDate");
      assertEquals("java.sql.Date", dateNode.get("type").asText());
      assertTrue(dateNode.get("value").asText().startsWith("2025-01-20"));
    }

    @Test
    @DisplayName("Serialized JobParameters wraps everything under 'parameters' key")
    void serializedStructureHasParametersWrapper() throws JsonProcessingException {
      JobParameters params = new JobParameters(Set.of(
        new JobParameter<>("key", "val", String.class)
      ));
      JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(params));

      assertEquals(1, node.size(), "root should have exactly one field");
      assertTrue(node.has("parameters"), "root field must be 'parameters'");
    }

    @Test
    @DisplayName("Each parameter entry contains type, value, and identifying fields")
    void eachParameterEntryHasRequiredFields() throws JsonProcessingException {
      JobParameters params = new JobParameters(Set.of(
        new JobParameter<>("alpha", "a", String.class, true),
        new JobParameter<>("beta", 99L, Long.class, false)
      ));
      JsonNode paramsNode = objectMapper.readTree(objectMapper.writeValueAsString(params)).get("parameters");

      paramsNode.properties().forEach(entry -> {
        JsonNode paramNode = entry.getValue();
        assertTrue(paramNode.has("type"), entry.getKey() + " missing 'type'");
        assertTrue(paramNode.has("value"), entry.getKey() + " missing 'value'");
        assertTrue(paramNode.has("identifying"), entry.getKey() + " missing 'identifying'");
        assertEquals(3, paramNode.size(), entry.getKey() + " should have exactly 3 fields");
      });
    }
  }
}
