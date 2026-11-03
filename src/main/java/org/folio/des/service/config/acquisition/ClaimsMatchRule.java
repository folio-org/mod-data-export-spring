package org.folio.des.service.config.acquisition;

import java.util.List;

import org.folio.des.domain.dto.ExportConfig;

/**
 * Every Claims config of the vendor applies. mod-orders creates one job per Claims config of the vendor and ignores
 * acquisition methods and accounts.
 */
class ClaimsMatchRule implements ConfigMatchRule {

  @Override
  public List<ExportConfig> match(List<ExportConfig> configsOfType, ConfigCriteria criteria) {
    return configsOfType;
  }

}
