package org.folio.des.service.config.acquisition;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.des.domain.dto.ConfigTransmissionMethod;
import org.folio.des.domain.dto.ExportType;

/**
 * The PO line attributes the export configs are matched against.
 */
public record PoLineCriteria(UUID vendorId, UUID acquisitionMethod, String vendorAccount, ExportType type,
                             ConfigTransmissionMethod transmissionMethod) {

  public boolean isEmpty() {
    return Stream.of(vendorId, acquisitionMethod, vendorAccount, type, transmissionMethod).allMatch(Objects::isNull);
  }

  public List<String> typeValues() {
    return type != null
      ? List.of(type.getValue())
      : List.of(ExportType.EDIFACT_ORDERS_EXPORT.getValue(), ExportType.CLAIMS.getValue());
  }

}
