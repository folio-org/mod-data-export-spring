package org.folio.des.service.config.acquisition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.ExportConfig;

/**
 * Without an acquisition method, every Claims config of the vendor applies. With one, a config that selects methods
 * applies only to those, and a config without selected methods applies to the methods no other Claims config selects.
 * Accounts and the default flag play no part.
 */
class ClaimsMatchRule implements EffectiveConfigMatchRule {

  @Override
  public List<ExportConfig> match(List<ExportConfig> configsOfType, ConfigCriteria criteria) {
    var method = criteria.acquisitionMethod();
    if (method == null) {
      return configsOfType;
    }
    var selectedMethods = configsOfType.stream()
      .flatMap(config -> acquisitionMethods(config).stream())
      .collect(Collectors.toSet());

    return configsOfType.stream()
      .filter(config -> {
        var methods = acquisitionMethods(config);
        return methods.isEmpty()
          ? EffectiveConfigMatchRule.isRest(method, selectedMethods)
          : methods.contains(method);
      })
      .toList();
  }

  private static List<UUID> acquisitionMethods(ExportConfig config) {
    return Optional.ofNullable(config.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getEdiConfig())
      .map(EdiConfig::getDefaultAcquisitionMethods)
      .orElse(List.of());
  }

}
