package org.folio.des.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfiguration {

  private static final ObjectMapper OBJECT_MAPPER;
  private static final ObjectMapper ENTITY_OBJECT_MAPPER;

  static {
    OBJECT_MAPPER =
        new ObjectMapper()
            .findAndRegisterModules()
            .registerModule(
                new SimpleModule()
                  .addDeserializer(ExitStatus.class, new ExitStatusDeserializer())
                  .addDeserializer(JobParameter.class, new JobParameterDeserializer())
                  .addSerializer(UUID.class, new UUIDSerializer(UUID.class)))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    ENTITY_OBJECT_MAPPER = OBJECT_MAPPER.copy()
      .setSerializationInclusion(JsonInclude.Include.ALWAYS);
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
        case "STRING" -> new JobParameter<>("STRING", jsonNode.get(VALUE_PARAMETER_PROPERTY).asText(), String.class, identifying);
        case "DATE" -> new JobParameter<>(
          "DATE", Date.valueOf(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText()), Date.class, identifying);
        case "LONG" -> new JobParameter<>("LONG", jsonNode.get(VALUE_PARAMETER_PROPERTY).asLong(), Long.class, identifying);
        case "DOUBLE" -> new JobParameter<>("DOUBLE", jsonNode.get(VALUE_PARAMETER_PROPERTY).asDouble(), Double.class, identifying);
      }
      return null;
    }
  }

  static class UUIDSerializer extends StdSerializer<UUID> {
    public UUIDSerializer(Class<UUID> t) {
      super(t);
    }

    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeString(value.toString());
    }
  }

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return OBJECT_MAPPER;
  }

  @Bean
  @Qualifier("entityObjectMapper")
  public ObjectMapper entityObjectMapper() {
    return ENTITY_OBJECT_MAPPER;
  }

}
