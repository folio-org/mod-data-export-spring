package org.folio.des.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.batch.core.ExitStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class JacksonConfiguration {

  private static final Map<String, ExitStatus> EXIT_STATUSES = new HashMap<>();

  static {
    EXIT_STATUSES.put("UNKNOWN", ExitStatus.UNKNOWN);
    EXIT_STATUSES.put("EXECUTING", ExitStatus.EXECUTING);
    EXIT_STATUSES.put("COMPLETED", ExitStatus.COMPLETED);
    EXIT_STATUSES.put("NOOP", ExitStatus.NOOP);
    EXIT_STATUSES.put("FAILED", ExitStatus.FAILED);
    EXIT_STATUSES.put("STOPPED", ExitStatus.STOPPED);
  }

  static class ExitStatusDeserializer extends StdDeserializer<ExitStatus> {

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

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().registerModule(new SimpleModule().addDeserializer(ExitStatus.class, new ExitStatusDeserializer()))
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

}
