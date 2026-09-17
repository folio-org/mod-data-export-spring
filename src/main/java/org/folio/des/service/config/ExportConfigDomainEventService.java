package org.folio.des.service.config;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.event.DomainEvent;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

/**
 * Builds {@link DomainEvent} envelopes for Export Configuration changes and hands them to the
 * {@link ExportConfigEventProducer}. Config services delegate here so they never build events directly.
 *
 * <p>Every snapshot is {@link #sanitize(ExportConfig) sanitized} before it leaves this service — the FTP
 * password (and any other credential-shaped field) is stripped from a deep copy so it can never reach the event
 * bus. The producer's {@code ObjectMapper} omits empty values, so the stripped fields do not appear at all
 * (not even as {@code null}) in the serialized payload.</p>
 */
@Log4j2
@Service
public class ExportConfigDomainEventService {

  private final ExportConfigEventProducer exportConfigEventProducer;
  private final FolioExecutionContext folioExecutionContext;
  private final ObjectMapper objectMapper;

  public ExportConfigDomainEventService(ExportConfigEventProducer exportConfigEventProducer,
                                        FolioExecutionContext folioExecutionContext,
                                        @Qualifier("entityObjectMapper") ObjectMapper objectMapper) {
    this.exportConfigEventProducer = exportConfigEventProducer;
    this.folioExecutionContext = folioExecutionContext;
    this.objectMapper = objectMapper;
  }

  /**
   * Publishes a {@code CREATE} Export Configuration event carrying the new snapshot.
   *
   * @param newConfig the newly-created configuration snapshot
   */
  public void publishConfigCreatedEvent(ExportConfig newConfig) {
    if (newConfig == null || StringUtils.isBlank(newConfig.getId())) {
      log.warn("publishConfigCreatedEvent:: skipping CREATE event, config id is missing");
      return;
    }
    var event = DomainEvent.createEvent(sanitize(newConfig), folioExecutionContext.getTenantId());
    publish(newConfig.getId(), event);
  }

  /**
   * Publishes an {@code UPDATE} Export Configuration event carrying both the pre- and post-change snapshots.
   *
   * @param oldConfig the pre-change configuration snapshot
   * @param newConfig the post-change configuration snapshot
   */
  public void publishConfigUpdatedEvent(ExportConfig oldConfig, ExportConfig newConfig) {
    if (newConfig == null || StringUtils.isBlank(newConfig.getId())) {
      log.warn("publishConfigUpdatedEvent:: skipping UPDATE event, config id is missing");
      return;
    }
    var event = DomainEvent.updateEvent(sanitize(oldConfig), sanitize(newConfig), folioExecutionContext.getTenantId());
    publish(newConfig.getId(), event);
  }

  private void publish(String configId, DomainEvent<ExportConfig> event) {
    log.debug("publish:: publishing config event [id: {}, type: {}, tenant: {}]",
      configId, event.getType(), event.getTenant());
    exportConfigEventProducer.publish(UUID.fromString(configId), event);
  }

  /**
   * Returns a deep copy of the given configuration with all credential-shaped fields removed, safe to publish on
   * the event bus. The original object is left untouched so the REST response still carries the full data.
   *
   * @param config the configuration snapshot to sanitize
   * @return a sanitized deep copy, or {@code null} if the input is {@code null}
   */
  private ExportConfig sanitize(ExportConfig config) {
    if (config == null) {
      return null;
    }
    var sanitized = objectMapper.convertValue(config, ExportConfig.class);
    Optional.ofNullable(sanitized)
      .map(ExportConfig::getExportTypeSpecificParameters)
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .map(VendorEdiOrdersExportConfig::getEdiFtp)
      .ifPresent(ediFtp -> ediFtp.setPassword(null));
    return sanitized;
  }
}