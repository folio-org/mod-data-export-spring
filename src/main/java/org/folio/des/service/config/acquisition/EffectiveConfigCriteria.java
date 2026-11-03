package org.folio.des.service.config.acquisition;

import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.ExportConfigTransmissionMethod;
import org.folio.des.domain.dto.ExportType;

/**
 * The attributes the export configs are matched against.
 */
public record EffectiveConfigCriteria(UUID vendorId, UUID acquisitionMethod, String vendorAccount, ExportType type,
                                   ExportConfigTransmissionMethod transmissionMethod) {

  public List<String> typeValues() {
    return type != null
      ? List.of(type.getValue())
      : List.of(ExportType.EDIFACT_ORDERS_EXPORT.getValue(), ExportType.CLAIMS.getValue());
  }

}
