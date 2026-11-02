package org.folio.des.service.config.acquisition;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.des.domain.dto.ExportConfigTransmissionMethod;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum;

/**
 * Selects the configs of a vendor that apply to a PO line.
 */
public class ExportConfigMatcher {

  private static final Map<ExportType, ConfigMatchRule> RULES = Map.of(
    ExportType.EDIFACT_ORDERS_EXPORT, new OrderingMatchRule(),
    ExportType.CLAIMS, new ClaimsMatchRule());

  /**
   * @param vendorConfigs all configs of the vendor, also those of other transmission methods: the rules need them
   */
  public List<ExportConfig> match(List<ExportConfig> vendorConfigs, ExportConfigCriteria criteria) {
    return vendorConfigs.stream()
      .collect(Collectors.groupingBy(ExportConfig::getType))
      .entrySet().stream()
      .filter(configsOfType -> RULES.containsKey(configsOfType.getKey()))
      .flatMap(configsOfType -> RULES.get(configsOfType.getKey()).match(configsOfType.getValue(), criteria).stream())
      .filter(config -> matchesTransmissionMethod(config, criteria.transmissionMethod()))
      .sorted(Comparator.comparing(ExportConfig::getConfigName))
      .toList();
  }

  private boolean matchesTransmissionMethod(ExportConfig config, ExportConfigTransmissionMethod transmissionMethod) {
    if (transmissionMethod == null) {
      return true;
    }
    TransmissionMethodEnum configMethod = config.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getTransmissionMethod();
    return configMethod != null && configMethod.getValue().equals(transmissionMethod.getValue());
  }

}
