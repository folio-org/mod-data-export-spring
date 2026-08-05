package org.folio.des.domain.dto.event;

import org.folio.des.domain.dto.EdiFtp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event-only view of {@link EdiFtp} that structurally omits the FTP {@code password} (and any other
 * credential-shaped field). Redaction is enforced by the type system: this record simply has no component to
 * carry a secret, so a credential can never reach the event bus regardless of what the source entity holds.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EdiFtpEventDto(
    @JsonProperty("ftpConnMode") EdiFtp.FtpConnModeEnum ftpConnMode,
    @JsonProperty("ftpFormat") EdiFtp.FtpFormatEnum ftpFormat,
    @JsonProperty("ftpMode") EdiFtp.FtpModeEnum ftpMode,
    @JsonProperty("ftpPort") Integer ftpPort,
    @JsonProperty("invoiceDirectory") String invoiceDirectory,
    @JsonProperty("isPrimaryTransmissionMethod") Boolean isPrimaryTransmissionMethod,
    @JsonProperty("notes") String notes,
    @JsonProperty("orderDirectory") String orderDirectory,
    @JsonProperty("serverAddress") String serverAddress,
    @JsonProperty("username") String username) {
}