package org.folio.des.service.config.acquisition;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.folio.des.domain.dto.EffectiveConfigType;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.TransmissionMethod;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum;

/**
 * Selects the configs of a vendor that apply to the config criteria.
 */
public class EffectiveConfigMatcher {

  private static final Map<EffectiveConfigType, EffectiveConfigMatchRule> RULES = Map.of(
    EffectiveConfigType.EDIFACT_ORDERS_EXPORT, new OrderingMatchRule(),
    EffectiveConfigType.CLAIMS, new ClaimsMatchRule());

  /**
   * @param vendorConfigs all configs of the vendor and the criteria's type, also those of other transmission methods:
   *                      the rules need them
   */
  public List<ExportConfig> match(List<ExportConfig> vendorConfigs, ConfigCriteria criteria) {
    return RULES.get(criteria.type()).match(vendorConfigs, criteria).stream()
      .filter(config -> matchesTransmissionMethod(config, criteria.transmissionMethod()))
      .sorted(Comparator.comparing(ExportConfig::getConfigName))
      .toList();
  }

  private boolean matchesTransmissionMethod(ExportConfig config, TransmissionMethod transmissionMethod) {
    if (transmissionMethod == null) {
      return true;
    }
    TransmissionMethodEnum configMethod = config.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getTransmissionMethod();
    return configMethod != null && configMethod.getValue().equals(transmissionMethod.getValue());
  }

}
