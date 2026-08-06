package org.folio.des.service.config;

import java.util.UUID;

import org.folio.des.config.kafka.KafkaService;
import org.folio.des.config.kafka.KafkaService.Topic;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.event.DomainEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Publishes Export Configuration {@link DomainEvent}s to the tenant-scoped Kafka topic.
 *
 * <p>The topic ({@code {env}.{tenant}.data-export-spring.config}) is created on tenant enable by the tenant service
 * via {@link KafkaService}. Publishing failures are caught and logged at ERROR — they never fail the originating
 * REST request or roll back the DB transaction.</p>
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class ExportConfigEventProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaService kafkaService;

  /**
   * Publishes an Export Configuration domain event to the tenant-scoped topic. The tenant is taken from the
   * event envelope so the topic resolution and the {@code tenant} field on the payload can never diverge.
   *
   * @param configId the export configuration id, used as the Kafka record key
   * @param event    the domain event envelope to publish
   */
  public void publish(UUID configId, DomainEvent<ExportConfig> event) {
    var topic = kafkaService.getTenantTopicName(Topic.CONFIG.getTopicName(), event.getTenant());
    var key = configId.toString();
    try {
      log.info("publish:: Publishing {} event for config id={} on topic={}", event.getType(), configId, topic);
      kafkaTemplate.send(topic, key, event);
      log.info("publish:: Successfully published {} event for config id={}", event.getType(), configId);
    } catch (Exception e) {
      log.error("publish:: Failed to publish {} event for config id={} on topic={}", event.getType(), configId, topic, e);
    }
  }
}