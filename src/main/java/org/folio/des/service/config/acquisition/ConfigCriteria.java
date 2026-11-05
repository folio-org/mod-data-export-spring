package org.folio.des.service.config.acquisition;

import java.util.UUID;

import org.folio.des.domain.dto.EffectiveConfigType;
import org.folio.des.domain.dto.TransmissionMethod;

/**
 * The attributes the export configs are matched against.
 *
 * @param scheduled true for only scheduled configs, false for only unscheduled ones, null for both
 */
public record ConfigCriteria(UUID vendorId, UUID acquisitionMethod, String vendorAccount, EffectiveConfigType type,
                                   TransmissionMethod transmissionMethod, Boolean scheduled) {
}
