package org.folio.des.service.config.acquisition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.TransmissionMethod;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EffectiveConfigMatcherTest {

  private static final UUID VENDOR = UUID.randomUUID();
  private static final UUID PURCHASE = UUID.randomUUID();
  private static final UUID EXCHANGE = UUID.randomUUID();

  private final EffectiveConfigMatcher matcher = new EffectiveConfigMatcher();

  @Test
  @DisplayName("Ordering config matches only the acquisition methods it selects")
  void orderingMatchesAcquisitionMethod() {
    var configs = List.of(ordering("A", List.of(PURCHASE), List.of()));

    assertThat(match(configs, PURCHASE, null)).containsExactly("A");
    assertThat(match(configs, EXCHANGE, null)).isEmpty();
  }

  @Test
  @DisplayName("No Ordering config matches without an acquisition method")
  void orderingNeedsAcquisitionMethod() {
    assertThat(match(List.of(ordering("A", List.of(PURCHASE), List.of())), null, null)).isEmpty();
    assertThat(match(List.of(defaultOrdering("B", List.of(PURCHASE), List.of())), null, null)).isEmpty();
  }

  @Test
  @DisplayName("Ordering config without an EDI config or lists matches nothing but does not fail")
  void orderingWithoutEdiConfig() {
    var noEdiConfig = ordering("A", List.of(PURCHASE), List.of());
    parameters(noEdiConfig).setEdiConfig(null);
    var noLists = ordering("B", List.of(PURCHASE), List.of());
    parameters(noLists).setEdiConfig(new EdiConfig().defaultAcquisitionMethods(null).accountNoList(null));

    assertThat(match(List.of(noEdiConfig, noLists), PURCHASE, "111")).isEmpty();
  }

  @Test
  @DisplayName("Non-default Ordering config with accounts matches only those accounts")
  void nonDefaultWithAccounts() {
    var configs = List.of(ordering("A", List.of(PURCHASE), List.of("111")));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("A");
    assertThat(match(configs, PURCHASE, "222")).isEmpty();
    assertThat(match(configs, PURCHASE, null)).isEmpty();
  }

  @Test
  @DisplayName("Non-default Ordering config without accounts matches every line")
  void nonDefaultWithoutAccounts() {
    var configs = List.of(ordering("A", List.of(PURCHASE), List.of()));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("A");
    assertThat(match(configs, PURCHASE, null)).containsExactly("A");
  }

  @Test
  @DisplayName("Only Ordering config of the vendor, default: every line, and its own hidden accounts are ignored")
  void defaultAlone() {
    var configs = List.of(defaultOrdering("B", List.of(PURCHASE), List.of("111")));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("B");
    assertThat(match(configs, PURCHASE, "222")).containsExactly("B");
    assertThat(match(configs, PURCHASE, null)).containsExactly("B");
  }

  @Test
  @DisplayName("Default Ordering config takes the accounts the other configs don't list")
  void defaultWithOtherAccounts() {
    var configs = List.of(
      ordering("A", List.of(PURCHASE), List.of("111")),
      defaultOrdering("B", List.of(PURCHASE), List.of()));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("A");
    assertThat(match(configs, PURCHASE, "222")).containsExactly("B");
    assertThat(match(configs, PURCHASE, null)).containsExactly("B");
  }

  @Test
  @DisplayName("Default Ordering config next to configs without accounts matches every line")
  void defaultWithOthersWithoutAccounts() {
    var configs = List.of(
      ordering("A", List.of(PURCHASE), List.of()),
      defaultOrdering("B", List.of(PURCHASE), List.of()));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("A", "B");
  }

  @Test
  @DisplayName("A default config's hidden accounts don't exclude lines from it or from another default config")
  void hiddenAccountsOfDefaultsAreIgnored() {
    var configs = List.of(
      defaultOrdering("A", List.of(PURCHASE), List.of("111")),
      defaultOrdering("B", List.of(PURCHASE), List.of("222")));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("A", "B");
  }

  @Test
  @DisplayName("Same as the worker: an account listed elsewhere is excluded regardless of that config's methods")
  void accountExcludedRegardlessOfAcquisitionMethod() {
    var configs = List.of(
      ordering("A", List.of(PURCHASE), List.of("111")),
      defaultOrdering("B", List.of(PURCHASE, EXCHANGE), List.of()));

    assertThat(match(configs, EXCHANGE, "111")).isEmpty();
  }

  @Test
  @DisplayName("Claims configs always match, whatever they select")
  void claimsAlwaysMatch() {
    var configs = List.of(claims("C", List.of(PURCHASE), List.of("111")));

    assertThat(matchByType(configs, ExportType.CLAIMS, EXCHANGE, "222")).containsExactly("C");
    assertThat(matchByType(configs, ExportType.CLAIMS, null, null)).containsExactly("C");
  }

  @Test
  @DisplayName("A type without a rule matches nothing")
  void typeWithoutRule() {
    var configs = List.of(ordering("A", List.of(PURCHASE), List.of()));

    assertThat(matchByType(configs, ExportType.BURSAR_FEES_FINES, PURCHASE, null)).isEmpty();
  }

  @Test
  @DisplayName("Several matches are sorted by config name")
  void severalMatchesSorted() {
    var configs = List.of(
      ordering("D", List.of(PURCHASE), List.of()),
      ordering("A", List.of(PURCHASE), List.of()));

    assertThat(match(configs, PURCHASE, null)).containsExactly("A", "D");
  }

  @Test
  @DisplayName("The transmission method narrows the matches after the rule ran")
  void transmissionMethodDoesNotChangeDefaultRule() {
    var ftpConfig = ordering("A", List.of(PURCHASE), List.of("111"));
    parameters(ftpConfig).setTransmissionMethod(TransmissionMethodEnum.FTP);
    var emailDefault = defaultOrdering("B", List.of(PURCHASE), List.of());
    parameters(emailDefault).setTransmissionMethod(TransmissionMethodEnum.EMAIL);
    var configs = List.of(ftpConfig, emailDefault);

    // the FTP config still excludes its account from the default although it is not requested
    assertThat(matchByMethod(configs, TransmissionMethod.EMAIL, "111")).isEmpty();
    assertThat(matchByMethod(configs, TransmissionMethod.EMAIL, "222")).containsExactly("B");
    assertThat(matchByMethod(configs, TransmissionMethod.FTP, "111")).containsExactly("A");
    assertThat(matchByMethod(configs, TransmissionMethod.FILE_DOWNLOAD, "111")).isEmpty();
  }

  private List<String> match(List<ExportConfig> configs, UUID acquisitionMethod, String vendorAccount) {
    return matchByType(configs, ExportType.EDIFACT_ORDERS_EXPORT, acquisitionMethod, vendorAccount);
  }

  private List<String> matchByType(List<ExportConfig> configs, ExportType type, UUID acquisitionMethod, String vendorAccount) {
    return names(matcher.match(configs, new ConfigCriteria(VENDOR, acquisitionMethod, vendorAccount, type, null)));
  }

  private List<String> matchByMethod(List<ExportConfig> configs, TransmissionMethod method, String vendorAccount) {
    return names(matcher.match(configs, new ConfigCriteria(VENDOR, PURCHASE, vendorAccount, ExportType.EDIFACT_ORDERS_EXPORT, method)));
  }

  private static List<String> names(List<ExportConfig> configs) {
    return configs.stream().map(ExportConfig::getConfigName).toList();
  }

  private static VendorEdiOrdersExportConfig parameters(ExportConfig config) {
    return config.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig();
  }

  private static ExportConfig ordering(String name, List<UUID> methods, List<String> accounts) {
    return config(name, ExportType.EDIFACT_ORDERS_EXPORT, methods, accounts, false);
  }

  private static ExportConfig defaultOrdering(String name, List<UUID> methods, List<String> accounts) {
    return config(name, ExportType.EDIFACT_ORDERS_EXPORT, methods, accounts, true);
  }

  private static ExportConfig claims(String name, List<UUID> methods, List<String> accounts) {
    return config(name, ExportType.CLAIMS, methods, accounts, false);
  }

  private static ExportConfig config(String name, ExportType type, List<UUID> methods, List<String> accounts, boolean isDefault) {
    var parameters = new VendorEdiOrdersExportConfig()
      .vendorId(VENDOR)
      .isDefaultConfig(isDefault)
      .ediConfig(new EdiConfig().defaultAcquisitionMethods(methods).accountNoList(accounts));
    return new ExportConfig()
      .configName(name)
      .type(type)
      .exportTypeSpecificParameters(new ExportTypeSpecificParameters().vendorEdiOrdersExportConfig(parameters));
  }

}
