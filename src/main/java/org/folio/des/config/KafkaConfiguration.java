package org.folio.des.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

  private final ObjectMapper objectMapper;

  @Bean
  public JsonDeserializer<?> jsonDeserializer() {
    return new JsonDeserializer<>(objectMapper);
  }

}
