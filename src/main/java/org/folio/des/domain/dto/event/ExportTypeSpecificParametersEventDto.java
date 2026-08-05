package org.folio.des.domain.dto.event;

import org.folio.des.domain.dto.AuthorityControlExportConfig;
import org.folio.des.domain.dto.BursarExportJob;
import org.folio.des.domain.dto.EHoldingsExportConfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event-only view of {@code ExportTypeSpecificParameters}. Only the credential-bearing branch
 * ({@code vendorEdiOrdersExportConfig}) is retyped to its redacted event variant; the remaining branches carry
 * no secrets and reuse the generated DTOs directly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportTypeSpecificParametersEventDto(
    @JsonProperty("bursarFeeFines") BursarExportJob bursarFeeFines,
    @JsonProperty("vendorEdiOrdersExportConfig") VendorEdiOrdersExportConfigEventDto vendorEdiOrdersExportConfig,
    @JsonProperty("query") String query,
    @JsonProperty("eHoldingsExportConfig") EHoldingsExportConfig eHoldingsExportConfig,
    @JsonProperty("authorityControlExportConfig") AuthorityControlExportConfig authorityControlExportConfig) {
}
