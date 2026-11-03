package org.folio.des.service.config.acquisition;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;

/**
 * The worker's Ordering config rule, MapToEdifactOrdersTasklet#getPoLineQuery and
 * #getVendorAccountFilter, with the account numbers of default configs ignored (MODEXPW-664).
 */
class OrderingMatchRule implements EffectiveConfigMatchRule {

  @Override
  public List<ExportConfig> match(List<ExportConfig> configsOfType, ConfigCriteria criteria) {
    var accountsOfNonDefaultConfigs = configsOfType.stream()
      .map(OrderingMatchRule::parameters)
      .filter(parameters -> !isDefault(parameters))
      .flatMap(parameters -> accounts(parameters).stream())
      .collect(Collectors.toSet());

    return configsOfType.stream()
      .filter(config -> matchesAcquisitionMethod(parameters(config), criteria))
      .filter(config -> matchesAccount(parameters(config), criteria, accountsOfNonDefaultConfigs))
      .toList();
  }

  private boolean matchesAcquisitionMethod(VendorEdiOrdersExportConfig parameters, ConfigCriteria criteria) {
    return criteria.acquisitionMethod() != null
      && ediConfig(parameters).map(EdiConfig::getDefaultAcquisitionMethods)
        .filter(methods -> methods.contains(criteria.acquisitionMethod()))
        .isPresent();
  }

  private boolean matchesAccount(VendorEdiOrdersExportConfig parameters, ConfigCriteria criteria,
                                 Set<String> accountsOfNonDefaultConfigs) {
    if (isDefault(parameters)) {
      return EffectiveConfigMatchRule.isRest(criteria.vendorAccount(), accountsOfNonDefaultConfigs);
    }
    var accounts = accounts(parameters);
    return accounts.isEmpty() || criteria.vendorAccount() != null && accounts.contains(criteria.vendorAccount());
  }

  private static VendorEdiOrdersExportConfig parameters(ExportConfig config) {
    return config.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig();
  }

  private static boolean isDefault(VendorEdiOrdersExportConfig parameters) {
    return Boolean.TRUE.equals(parameters.getIsDefaultConfig());
  }

  private static List<String> accounts(VendorEdiOrdersExportConfig parameters) {
    return ediConfig(parameters).map(EdiConfig::getAccountNoList).orElse(List.of());
  }

  private static Optional<EdiConfig> ediConfig(VendorEdiOrdersExportConfig parameters) {
    return Optional.ofNullable(parameters.getEdiConfig());
  }

}
