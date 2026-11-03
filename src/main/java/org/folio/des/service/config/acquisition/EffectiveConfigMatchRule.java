package org.folio.des.service.config.acquisition;

import java.util.Collection;
import java.util.List;

import org.folio.des.domain.dto.ExportConfig;

/**
 * Decides which configs of one export type apply to the config criteria. Gets all configs of that type of the vendor, because a
 * catch-all config depends on what the other configs select.
 */
interface EffectiveConfigMatchRule {

  List<ExportConfig> match(List<ExportConfig> configsOfType, ConfigCriteria criteria);

  /**
   * Specific lists win, the rest goes to the catch-all: a catch-all takes every value that no specific config selects,
   * including no value at all.
   */
  static boolean isRest(Object value, Collection<?> selectedBySpecificConfigs) {
    return !selectedBySpecificConfigs.contains(value);
  }

}
