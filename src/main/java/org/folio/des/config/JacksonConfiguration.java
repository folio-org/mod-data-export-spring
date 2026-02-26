package org.folio.des.config;

import com.fasterxml.jackson.annotation.JsonInclude;

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
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

@Configuration
public class JacksonConfiguration {

  private static final ObjectMapper OBJECT_MAPPER;
  private static final ObjectMapper ENTITY_OBJECT_MAPPER;

  static {
    OBJECT_MAPPER =
        JsonMapper.builder()
            .findAndAddModules()
            .addModule(
                new SimpleModule()
                  .addDeserializer(ExitStatus.class, new ExitStatusDeserializer())
                  .addDeserializer(JobParameter.class, new JobParameterDeserializer())
                  .addSerializer(UUID.class, new UUIDSerializer(UUID.class)))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
          .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_EMPTY))
          .build();
    ENTITY_OBJECT_MAPPER = OBJECT_MAPPER.rebuild()
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
      .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.ALWAYS))
      .build();
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
      this(JavaType.class);
    }

    public ExitStatusDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public ExitStatus deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
      return EXIT_STATUSES.get(((JsonNode) jp.objectReadContext().readTree(jp)).get("exitCode").asString());
    }

  }

  static class JobParameterDeserializer extends StdDeserializer<JobParameter<?>> {

    private static final String VALUE_PARAMETER_PROPERTY = "value";

    public JobParameterDeserializer() {
      this(JavaType.class);
    }

    public JobParameterDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public JobParameter<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
      JsonNode jsonNode = jp.objectReadContext().readTree(jp);
      var identifying = jsonNode.get("identifying").asBoolean();
      switch (jsonNode.get("type").asString()) {
        case "STRING" -> new JobParameter<>("STRING", jsonNode.get(VALUE_PARAMETER_PROPERTY).asString(),
          String.class, identifying);
        case "DATE" -> new JobParameter<>("DATE",
          Date.valueOf(jsonNode.get(VALUE_PARAMETER_PROPERTY).asString()), Date.class, identifying);
        case "LONG" -> new JobParameter<>("LONG", jsonNode.get(VALUE_PARAMETER_PROPERTY).asLong(),
          Long.class, identifying);
        case "DOUBLE" -> new JobParameter<>("DOUBLE", jsonNode.get(VALUE_PARAMETER_PROPERTY).asDouble(),
          Double.class, identifying);
      }
      return null;
    }
  }

  static class UUIDSerializer extends StdSerializer<UUID> {
    public UUIDSerializer(Class<UUID> t) {
      super(t);
    }

    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
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

  public ObjectMapper get() {
    return OBJECT_MAPPER;
  }

}
