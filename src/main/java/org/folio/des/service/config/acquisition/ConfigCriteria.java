package org.folio.des.service.config.acquisition;

import java.util.UUID;

import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.TransmissionMethod;

/**
 * The attributes the export configs are matched against.
 */
public record ConfigCriteria(UUID vendorId, UUID acquisitionMethod, String vendorAccount, ExportType type,
                                   TransmissionMethod transmissionMethod) {
}
