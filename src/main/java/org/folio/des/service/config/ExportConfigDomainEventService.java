package org.folio.des.service.config;

import java.util.UUID;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.event.DomainEvent;
import org.folio.des.domain.dto.event.ExportConfigEventDto;
import org.folio.des.mapper.event.ExportConfigEventMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Builds {@link DomainEvent} envelopes for Export Configuration changes and hands them to the
 * {@link ExportConfigEventProducer}. Config services delegate here so they never build events directly.
 *
 * <p>The full REST {@link ExportConfig} snapshot is mapped through {@link ExportConfigEventMapper} to the
 * credential-free {@link ExportConfigEventDto} before it leaves this service — redaction happens exactly once,
 * here, and is enforced by the target type rather than by a runtime stripping step.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ExportConfigDomainEventService {

  private final ExportConfigEventProducer exportConfigEventProducer;
  private final ExportConfigEventMapper exportConfigEventMapper;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Publishes a {@code CREATE} Export Configuration event carrying the new snapshot.
   *
   * @param newConfig the newly-created configuration snapshot
   */
  public void publishConfigCreatedEvent(ExportConfig newConfig) {
    var event = DomainEvent.createEvent(exportConfigEventMapper.toEventDto(newConfig), folioExecutionContext.getTenantId());
    publish(newConfig.getId(), event);
  }

  /**
   * Publishes an {@code UPDATE} Export Configuration event carrying both the pre- and post-change snapshots.
   *
   * @param oldConfig the pre-change configuration snapshot
   * @param newConfig the post-change configuration snapshot
   */
  public void publishConfigUpdatedEvent(ExportConfig oldConfig, ExportConfig newConfig) {
    var event = DomainEvent.updateEvent(exportConfigEventMapper.toEventDto(oldConfig),
      exportConfigEventMapper.toEventDto(newConfig), folioExecutionContext.getTenantId());
    publish(newConfig.getId(), event);
  }

  private void publish(String configId, DomainEvent<ExportConfigEventDto> event) {
    log.debug("publish:: publishing config event [id: {}, type: {}, tenant: {}]",
      configId, event.getType(), event.getTenant());
    exportConfigEventProducer.publish(UUID.fromString(configId), event);
  }
}