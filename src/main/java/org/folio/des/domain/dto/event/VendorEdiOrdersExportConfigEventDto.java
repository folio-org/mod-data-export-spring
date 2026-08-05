package org.folio.des.domain.dto.event;

import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiEmail;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event-only view of {@link VendorEdiOrdersExportConfig}. Identical to the REST DTO except that {@code ediFtp}
 * is typed as the redacted {@link EdiFtpEventDto} so no FTP credential can be carried on the event payload.
 * Non credential-bearing nested types ({@link EdiConfig}, {@link EdiEmail}, {@link EdiSchedule}) are reused
 * directly — none of them declare a credential-shaped field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorEdiOrdersExportConfigEventDto(
    @JsonProperty("exportConfigId") UUID exportConfigId,
    @JsonProperty("vendorId") UUID vendorId,
    @JsonProperty("configName") String configName,
    @JsonProperty("configDescription") String configDescription,
    @JsonProperty("ediConfig") EdiConfig ediConfig,
    @JsonProperty("ediEmail") EdiEmail ediEmail,
    @JsonProperty("ediFtp") EdiFtpEventDto ediFtp,
    @JsonProperty("ediSchedule") EdiSchedule ediSchedule,
    @JsonProperty("isDefaultConfig") Boolean isDefaultConfig,
    @JsonProperty("integrationType") VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType,
    @JsonProperty("transmissionMethod") VendorEdiOrdersExportConfig.TransmissionMethodEnum transmissionMethod,
    @JsonProperty("fileFormat") VendorEdiOrdersExportConfig.FileFormatEnum fileFormat,
    @JsonProperty("claimPieceIds") List<String> claimPieceIds) {
}
