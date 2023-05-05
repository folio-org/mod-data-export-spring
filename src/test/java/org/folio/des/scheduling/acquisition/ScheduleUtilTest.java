package org.folio.des.scheduling.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.folio.okapi.common.SemVer;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScheduleUtilTest {
  private static final SemVer sinceVersion = new SemVer("3.0.0-SNAPSHOT");

  @ParameterizedTest
  @CsvSource({
    "mod-2.1.0,            mod-3.0.0-SNAPSHOT,   true",
    "mod-2.1.0,            mod-3.0.0,            true",
    "2.1.0,                3.0.0,                true",
    "mod-2.1.0,            mod-3.0.0-SNAPSHOT.1, true",
    "mod-2.1.0,            mod-4.0.0,            true",
    "mod-3.0.0,            mod-3.1.0,            false",
    "mod-3.0.0,            mod-4.0.0,            false",
    "mod-4.0.0,            mod-5.0.0,            false",
    "mod-2.1.0,            mod-2.1.1,            false",
    "mod-3.0.0-SNAPSHOT,   mod-3.0.0-SNAPSHOT,   false",
    "mod-3.0.0-SNAPSHOT.1, mod-3.0.0-SNAPSHOT.2, false",
    "mod-3.0.0-SNAPSHOT,   mod-2.1.0,            false",
  })
  void shouldLoadScheduleConfigsOnUpgradeWhenQuartzEnabled(String moduleFrom, String moduleTo, boolean expected) {
    TenantAttributes tenantAttributes = new TenantAttributes().moduleFrom(moduleFrom).moduleTo(moduleTo);
    assertEquals(expected, ScheduleUtil.shouldLoadScheduleConfigs(tenantAttributes, true, sinceVersion));
  }

  @ParameterizedTest
  @CsvSource({
    "mod-2.1.0,            mod-3.0.0,            true",
    "mod-3.0.0,            mod-4.0.0,            true",
    "mod-2.1.0,            mod-2.1.1,            true",
  })
  void shouldAlwaysLoadScheduleConfigsWhenQuartzDisabled(String moduleFrom, String moduleTo, boolean expected) {
    TenantAttributes tenantAttributes = new TenantAttributes().moduleFrom(moduleFrom).moduleTo(moduleTo);
    assertEquals(expected, ScheduleUtil.shouldLoadScheduleConfigs(tenantAttributes, false, sinceVersion));
  }

  @ParameterizedTest
  @CsvSource({
    "mod-2.1.0,            mod-3.0.0,            true",
    "mod-3.0.0,            mod-4.0.0,            true",
    "mod-2.1.0,            mod-2.1.1,            true",
  })
  void shouldAlwaysLoadScheduleConfigsWhenForceParamSetToTrue(String moduleFrom, String moduleTo, boolean expected) {
    TenantAttributes tenantAttributes = new TenantAttributes().moduleFrom(moduleFrom).moduleTo(moduleTo)
      .parameters(List.of(new Parameter().key("forceSchedulesReload").value("true")));
    assertEquals(expected, ScheduleUtil.shouldLoadScheduleConfigs(tenantAttributes, true, sinceVersion));
  }

  @ParameterizedTest
  @CsvSource({
    "mod-2.1.0,            mod-3.0.0,            true",
    "mod-3.0.0,            mod-4.0.0,            false",
    "mod-2.1.0,            mod-2.1.1,            false",
  })
  void shouldLoadScheduleConfigsOnUpgradeWhenWhenForceParamSetToFalse(String moduleFrom, String moduleTo, boolean expected) {
    TenantAttributes tenantAttributes = new TenantAttributes().moduleFrom(moduleFrom).moduleTo(moduleTo)
      .parameters(List.of(new Parameter().key("forceSchedulesReload").value("false")));
    assertEquals(expected, ScheduleUtil.shouldLoadScheduleConfigs(tenantAttributes, true, sinceVersion));
  }

  @ParameterizedTest
  @CsvSource({
    "mod-3.0.0-SNAPSHOT, true",
    "mod-3.0.0,          true",
    "mod-4.0.0,          true",
    "mod-2.1.0,          false"
  })
  void shouldLoadScheduleConfigsWhenModuleFromNotDefined(String moduleTo, boolean expected) {
    TenantAttributes tenantAttributes = new TenantAttributes().moduleFrom(null).moduleTo(moduleTo);
    assertEquals(expected, ScheduleUtil.shouldLoadScheduleConfigs(tenantAttributes, true, sinceVersion));
  }

  @ParameterizedTest
  @CsvSource({
    "mod-3.0.0-SNAPSHOT, false",
    "mod-3.0.0,          false",
    "mod-4.0.0,          false",
    "mod-2.1.0,          false"
  })
  void shouldNotLoadScheduleConfigsWhenModuleToNotDefined(String moduleFrom, boolean expected) {
    TenantAttributes tenantAttributes = new TenantAttributes().moduleFrom(moduleFrom).moduleTo(null);
    assertEquals(expected, ScheduleUtil.shouldLoadScheduleConfigs(tenantAttributes, true, sinceVersion));
  }
}
