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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
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
                  .addSerializer(JobParameters.class, new JobParametersSerializer())
                  .addSerializer(new JobParameterSerializer())
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
      return switch (jsonNode.get("type").asText()) {
        case "STRING" -> new JobParameter<>("STRING", jsonNode.get(VALUE_PARAMETER_PROPERTY).asText(), String.class, identifying);
        case "DATE" -> new JobParameter<>(
          "DATE", Date.valueOf(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText()), Date.class, identifying);
        case "LONG" -> new JobParameter<>("LONG", jsonNode.get(VALUE_PARAMETER_PROPERTY).asLong(), Long.class, identifying);
        case "DOUBLE" -> new JobParameter<>("DOUBLE", jsonNode.get(VALUE_PARAMETER_PROPERTY).asDouble(), Double.class, identifying);
        default -> null;
      };
    }
  }

  static class JobParametersSerializer extends StdSerializer<JobParameters> {

    public JobParametersSerializer() {
      super(JobParameters.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(JobParameters value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeStartObject();
      gen.writeObjectFieldStart("parameters");
      
      if (value != null && !value.isEmpty()) {
        try {
          // Use reflection to access the internal map in Spring Batch 5 JobParameters
          java.lang.reflect.Field parametersField = JobParameters.class.getDeclaredField("parameters");
          parametersField.setAccessible(true);
          Map<String, JobParameter<?>> parametersMap = (Map<String, JobParameter<?>>) parametersField.get(value);
          
          for (Map.Entry<String, JobParameter<?>> entry : parametersMap.entrySet()) {
            String paramName = entry.getKey();
            JobParameter<?> param = entry.getValue();
            
            gen.writeObjectFieldStart(paramName);
            
            // Use reflection to access the type field
            java.lang.reflect.Field typeField = JobParameter.class.getDeclaredField("type");
            typeField.setAccessible(true);
            Class<?> type = (Class<?>) typeField.get(param);
            gen.writeStringField("type", type.getName());
            
            // Serialize value based on type
            Object paramValue = param.value();
            if (paramValue instanceof java.util.Date) {
              SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
              gen.writeStringField("value", sdf.format(paramValue));
            } else {
              gen.writeObjectField("value", paramValue);
            }
            
            gen.writeBooleanField("identifying", param.identifying());
            gen.writeEndObject();
          }
        } catch (NoSuchFieldException | IllegalAccessException e) {
          throw new IOException("Failed to serialize JobParameters", e);
        }
      }
      
      gen.writeEndObject();
      gen.writeEndObject();
    }
  }

  static class JobParameterSerializer extends StdSerializer<JobParameter<?>> {

    @SuppressWarnings("unchecked")
    public JobParameterSerializer() {
      super((Class<JobParameter<?>>) (Class<?>) JobParameter.class);
    }

    @Override
    public void serialize(JobParameter<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeStartObject();
      
      try {
        // Use reflection to access the type field
        java.lang.reflect.Field typeField = JobParameter.class.getDeclaredField("type");
        typeField.setAccessible(true);
        Class<?> type = (Class<?>) typeField.get(value);
        gen.writeStringField("type", type.getName());
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new IOException("Failed to get type from JobParameter", e);
      }
      
      // Serialize value based on type
      Object paramValue = value.value();
      if (paramValue instanceof java.util.Date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        gen.writeStringField("value", sdf.format(paramValue));
      } else {
        gen.writeObjectField("value", paramValue);
      }
      
      gen.writeBooleanField("identifying", value.identifying());
      gen.writeEndObject();
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

  @Bean
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(ObjectMapper objectMapper) {
    return hibernateProperties -> hibernateProperties.put(
            "hibernate.type.json_format_mapper",
            new JacksonJsonFormatMapper(objectMapper)
    );
  }

}
