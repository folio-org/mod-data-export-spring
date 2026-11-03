package org.folio.des.service.config.acquisition;

import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.TransmissionMethod;

/**
 * The attributes the export configs are matched against.
 */
public record ConfigCriteria(UUID vendorId, UUID acquisitionMethod, String vendorAccount, ExportType type,
                                   TransmissionMethod transmissionMethod) {

  public List<String> typeValues() {
    return type != null
      ? List.of(type.getValue())
      : List.of(ExportType.EDIFACT_ORDERS_EXPORT.getValue(), ExportType.CLAIMS.getValue());
  }

}
