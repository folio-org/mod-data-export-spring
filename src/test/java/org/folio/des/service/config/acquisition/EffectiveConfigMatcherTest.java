package org.folio.des.service.config.acquisition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.TransmissionMethod;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.EffectiveConfigType;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    assertThat(match(configs, PURCHASE, "")).isEmpty();
    assertThat(match(configs, PURCHASE, null)).containsExactly("A");
  }

  @Test
  @DisplayName("An absent account applies no account filter, but the acquisition method still has to match")
  void absentAccountAppliesNoFilter() {
    var configs = List.of(
      ordering("A", List.of(PURCHASE), List.of("111")),
      ordering("B", List.of(PURCHASE), List.of()),
      defaultOrdering("C", List.of(PURCHASE), List.of()),
      ordering("D", List.of(EXCHANGE), List.of("222")));

    assertThat(match(configs, PURCHASE, null)).containsExactly("A", "B", "C");
    assertThat(match(configs, EXCHANGE, null)).containsExactly("D");
  }

  @Test
  @DisplayName("Non-default Ordering config without accounts matches every line")
  void nonDefaultWithoutAccounts() {
    var configs = List.of(ordering("A", List.of(PURCHASE), List.of()));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("A");
    assertThat(match(configs, PURCHASE, "")).containsExactly("A");
    assertThat(match(configs, PURCHASE, null)).containsExactly("A");
  }

  @Test
  @DisplayName("Only Ordering config of the vendor, default: every line, and its own hidden accounts are ignored")
  void defaultAlone() {
    var configs = List.of(defaultOrdering("B", List.of(PURCHASE), List.of("111")));

    assertThat(match(configs, PURCHASE, "111")).containsExactly("B");
    assertThat(match(configs, PURCHASE, "222")).containsExactly("B");
    assertThat(match(configs, PURCHASE, "")).containsExactly("B");
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
    assertThat(match(configs, PURCHASE, "")).containsExactly("B");
    assertThat(match(configs, PURCHASE, null)).containsExactly("A", "B");
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
  @DisplayName("Without an acquisition method every Claims config matches")
  void claimsWithoutAcquisitionMethod() {
    var configs = List.of(claims("A", List.of(PURCHASE)), claims("B", List.of()));

    assertThat(matchClaims(configs, null)).containsExactly("A", "B");
  }

  @Test
  @DisplayName("Claims config with selected methods matches only those")
  void claimsSpecificOnly() {
    var configs = List.of(claims("A", List.of(PURCHASE)));

    assertThat(matchClaims(configs, PURCHASE)).containsExactly("A");
    assertThat(matchClaims(configs, EXCHANGE)).isEmpty();
  }

  @Test
  @DisplayName("Claims config without selected methods takes only the methods no other config selects")
  void claimsSpecificWinsOverCatchAll() {
    var configs = List.of(claims("A", List.of(PURCHASE)), claims("B", List.of()));

    assertThat(matchClaims(configs, PURCHASE)).containsExactly("A");
    assertThat(matchClaims(configs, EXCHANGE)).containsExactly("B");
  }

  @Test
  @DisplayName("Several Claims configs without selected methods each take the same rest")
  void claimsSeveralCatchAlls() {
    var configs = List.of(claims("A", List.of(PURCHASE)), claims("B", List.of()), claims("C", List.of()));

    assertThat(matchClaims(configs, PURCHASE)).containsExactly("A");
    assertThat(matchClaims(configs, EXCHANGE)).containsExactly("B", "C");
  }

  @Test
  @DisplayName("A method selected on several Claims configs matches all of them but not the catch-all")
  void claimsOverlap() {
    var configs = List.of(claims("A", List.of(PURCHASE)), claims("B", List.of(PURCHASE)), claims("C", List.of()));

    assertThat(matchClaims(configs, PURCHASE)).containsExactly("A", "B");
  }

  @Test
  @DisplayName("Claims config without an EDI config or method list is a catch-all")
  void claimsWithoutEdiConfig() {
    var noEdiConfig = claims("A", List.of());
    parameters(noEdiConfig).setEdiConfig(null);
    var noList = claims("B", List.of());
    parameters(noList).setEdiConfig(new EdiConfig().defaultAcquisitionMethods(null));
    var configs = List.of(noEdiConfig, noList, claims("C", List.of(PURCHASE)));

    assertThat(matchClaims(configs, EXCHANGE)).containsExactly("A", "B");
    assertThat(matchClaims(configs, PURCHASE)).containsExactly("C");
  }

  @Test
  @DisplayName("Accounts and the default flag play no part for Claims")
  void claimsIgnoreAccountsAndDefault() {
    var specific = config("A", ExportType.CLAIMS, List.of(PURCHASE), List.of("111"), false);
    var catchAll = config("B", ExportType.CLAIMS, List.of(), List.of("222"), false);
    var specificDefault = config("C", ExportType.CLAIMS, List.of(EXCHANGE), List.of(), true);
    var configs = List.of(specific, catchAll, specificDefault);

    assertThat(matchByType(configs, EffectiveConfigType.CLAIMS, PURCHASE, "222")).containsExactly("A");
    assertThat(matchByType(configs, EffectiveConfigType.CLAIMS, EXCHANGE, "111")).containsExactly("C");
    assertThat(matchByType(configs, EffectiveConfigType.CLAIMS, UUID.randomUUID(), "111")).containsExactly("B");
  }

  @Test
  @DisplayName("The transmission method narrows the Claims matches after the rule ran")
  void claimsTransmissionMethodDoesNotChangeRule() {
    var ftpConfig = claims("A", List.of(PURCHASE));
    parameters(ftpConfig).setTransmissionMethod(TransmissionMethodEnum.FTP);
    var emailCatchAll = claims("B", List.of());
    parameters(emailCatchAll).setTransmissionMethod(TransmissionMethodEnum.EMAIL);
    var configs = List.of(ftpConfig, emailCatchAll);

    // the FTP config still takes its method away from the catch-all although it is not requested
    assertThat(matchByMethod(configs, EffectiveConfigType.CLAIMS, TransmissionMethod.EMAIL, null)).isEmpty();
    assertThat(matchByMethod(configs, EffectiveConfigType.CLAIMS, TransmissionMethod.FTP, null)).containsExactly("A");
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
    assertThat(matchByMethod(configs, EffectiveConfigType.EDIFACT_ORDERS_EXPORT, TransmissionMethod.EMAIL, "111")).isEmpty();
    assertThat(matchByMethod(configs, EffectiveConfigType.EDIFACT_ORDERS_EXPORT, TransmissionMethod.EMAIL, "222")).containsExactly("B");
    assertThat(matchByMethod(configs, EffectiveConfigType.EDIFACT_ORDERS_EXPORT, TransmissionMethod.FTP, "111")).containsExactly("A");
    assertThat(matchByMethod(configs, EffectiveConfigType.EDIFACT_ORDERS_EXPORT, TransmissionMethod.FILE_DOWNLOAD, "111")).isEmpty();
  }

  @ParameterizedTest
  @CsvSource(nullValues = "null", value = {
    "111, null,  A",
    "111, true,  A",
    "111, false, ",
    "222, null,  B",
    "222, true,  B",
    "222, false, ",
    "333, null,  C",
    "333, true,  ",
    "333, false, C",
  })
  @DisplayName("scheduled narrows the matches after the rule ran: C has no schedule but still excludes its account from the default")
  void scheduledNarrowsMatches(String account, Boolean scheduled, String expected) {
    var configs = List.of(
      scheduled(ordering("A", List.of(PURCHASE), List.of("111"))),
      scheduled(defaultOrdering("B", List.of(PURCHASE), List.of())),
      ordering("C", List.of(PURCHASE), List.of("333")));

    assertThat(matchBySchedule(configs, account, scheduled))
      .isEqualTo(expected == null ? List.of() : List.of(expected));
  }

  @Test
  @DisplayName("An Ordering config counts as scheduled only if the export is enabled and has a period other than NONE")
  void orderingScheduleStates() {
    var enabled = withSchedule(ordering("A", List.of(PURCHASE), List.of()), true, SchedulePeriodEnum.WEEK);
    var disabled = withSchedule(ordering("B", List.of(PURCHASE), List.of()), false, SchedulePeriodEnum.WEEK);
    var unsetFlag = withSchedule(ordering("C", List.of(PURCHASE), List.of()), null, SchedulePeriodEnum.WEEK);
    var periodNone = withSchedule(ordering("D", List.of(PURCHASE), List.of()), true, SchedulePeriodEnum.NONE);
    var noPeriod = withSchedule(ordering("E", List.of(PURCHASE), List.of()), true, null);
    var noParameters = ordering("F", List.of(PURCHASE), List.of());
    parameters(noParameters).setEdiSchedule(new EdiSchedule().enableScheduledExport(true));
    var noSchedule = ordering("G", List.of(PURCHASE), List.of());
    var configs = List.of(enabled, disabled, unsetFlag, periodNone, noPeriod, noParameters, noSchedule);

    assertThat(matchBySchedule(configs, null, true)).containsExactly("A");
    assertThat(matchBySchedule(configs, null, false)).containsExactly("B", "C", "D", "E", "F", "G");
    assertThat(matchBySchedule(configs, null, null)).containsExactly("A", "B", "C", "D", "E", "F", "G");
  }

  @Test
  @DisplayName("scheduled and transmissionMethod combine")
  void scheduledWithTransmissionMethod() {
    var scheduledFtp = scheduled(ordering("A", List.of(PURCHASE), List.of()));
    parameters(scheduledFtp).setTransmissionMethod(TransmissionMethodEnum.FTP);
    var scheduledEmail = scheduled(ordering("B", List.of(PURCHASE), List.of()));
    parameters(scheduledEmail).setTransmissionMethod(TransmissionMethodEnum.EMAIL);
    var unscheduledFtp = ordering("C", List.of(PURCHASE), List.of());
    parameters(unscheduledFtp).setTransmissionMethod(TransmissionMethodEnum.FTP);
    var configs = List.of(scheduledFtp, scheduledEmail, unscheduledFtp);

    var criteria = new ConfigCriteria(VENDOR, PURCHASE, null, EffectiveConfigType.EDIFACT_ORDERS_EXPORT, TransmissionMethod.FTP, true);

    assertThat(names(matcher.match(configs, criteria))).containsExactly("A");
  }

  @Test
  @DisplayName("Claims configs are never scheduled, whatever their stored schedule says")
  void claimsAreNeverScheduled() {
    var configs = List.of(scheduled(claims("A", List.of())));

    assertThat(names(matcher.match(configs, new ConfigCriteria(VENDOR, null, null, EffectiveConfigType.CLAIMS, null, true)))).isEmpty();
    assertThat(names(matcher.match(configs, new ConfigCriteria(VENDOR, null, null, EffectiveConfigType.CLAIMS, null, false)))).containsExactly("A");
    assertThat(names(matcher.match(configs, new ConfigCriteria(VENDOR, null, null, EffectiveConfigType.CLAIMS, null, null)))).containsExactly("A");
  }

  private List<String> match(List<ExportConfig> configs, UUID acquisitionMethod, String vendorAccount) {
    return matchByType(configs, EffectiveConfigType.EDIFACT_ORDERS_EXPORT, acquisitionMethod, vendorAccount);
  }

  private List<String> matchByType(List<ExportConfig> configs, EffectiveConfigType type, UUID acquisitionMethod, String vendorAccount) {
    return names(matcher.match(configs, new ConfigCriteria(VENDOR, acquisitionMethod, vendorAccount, type, null, null)));
  }

  private List<String> matchBySchedule(List<ExportConfig> configs, String vendorAccount, Boolean scheduled) {
    var criteria = new ConfigCriteria(VENDOR, PURCHASE, vendorAccount, EffectiveConfigType.EDIFACT_ORDERS_EXPORT, null, scheduled);
    return names(matcher.match(configs, criteria));
  }

  private static ExportConfig scheduled(ExportConfig config) {
    return withSchedule(config, true, SchedulePeriodEnum.DAY);
  }

  private static ExportConfig withSchedule(ExportConfig config, Boolean enabled, SchedulePeriodEnum period) {
    parameters(config).setEdiSchedule(new EdiSchedule()
      .enableScheduledExport(enabled)
      .scheduleParameters(new ScheduleParameters().schedulePeriod(period)));
    return config;
  }

  private List<String> matchClaims(List<ExportConfig> configs, UUID acquisitionMethod) {
    return matchByType(configs, EffectiveConfigType.CLAIMS, acquisitionMethod, null);
  }

  private List<String> matchByMethod(List<ExportConfig> configs, EffectiveConfigType type, TransmissionMethod method, String vendorAccount) {
    return names(matcher.match(configs, new ConfigCriteria(VENDOR, PURCHASE, vendorAccount, type, method, null)));
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

  private static ExportConfig claims(String name, List<UUID> methods) {
    return config(name, ExportType.CLAIMS, methods, List.of(), false);
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
