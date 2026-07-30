package org.folio.des.service.config;

import java.util.UUID;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.event.DomainEvent;
import org.folio.des.domain.dto.event.DomainEventType;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Builds {@link DomainEvent} envelopes for Export Configuration changes and hands them to the
 * {@link ExportConfigEventProducer}. Config services delegate here so they never build events directly.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ExportConfigDomainEventService {

  private final ExportConfigEventProducer exportConfigEventProducer;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Publishes a {@code CREATE} Export Configuration event carrying the new snapshot.
   *
   * @param newConfig the newly-created, sanitized configuration snapshot
   */
  public void publishConfigCreatedEvent(ExportConfig newConfig) {
    publish(newConfig.getId(), createEvent(newConfig));
  }

  /**
   * Publishes an {@code UPDATE} Export Configuration event carrying both the pre- and post-change snapshots.
   *
   * @param oldConfig the pre-change, sanitized configuration snapshot
   * @param newConfig the post-change, sanitized configuration snapshot
   */
  public void publishConfigUpdatedEvent(ExportConfig oldConfig, ExportConfig newConfig) {
    publish(newConfig.getId(), updateEvent(oldConfig, newConfig));
  }

  private DomainEvent<ExportConfig> createEvent(ExportConfig newConfig) {
    return DomainEvent.<ExportConfig>builder()
      .eventId(UUID.randomUUID())
      .eventTs(System.currentTimeMillis())
      .tenant(folioExecutionContext.getTenantId())
      .type(DomainEventType.CREATE)
      .newValue(newConfig)
      .build();
  }

  private DomainEvent<ExportConfig> updateEvent(ExportConfig oldConfig, ExportConfig newConfig) {
    return DomainEvent.<ExportConfig>builder()
      .eventId(UUID.randomUUID())
      .eventTs(System.currentTimeMillis())
      .tenant(folioExecutionContext.getTenantId())
      .type(DomainEventType.UPDATE)
      .oldValue(oldConfig)
      .newValue(newConfig)
      .build();
  }

  private void publish(String configId, DomainEvent<ExportConfig> event) {
    log.debug("publish:: publishing config event [id: {}, type: {}, tenant: {}]",
      configId, event.getType(), event.getTenant());
    exportConfigEventProducer.publish(configId, event);
  }
}
