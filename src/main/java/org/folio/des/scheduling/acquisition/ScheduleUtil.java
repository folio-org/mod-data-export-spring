package org.folio.des.scheduling.acquisition;

import java.util.Collection;
import java.util.stream.Stream;

import org.folio.okapi.common.ModuleId;
import org.folio.okapi.common.SemVer;
import org.folio.tenant.domain.dto.TenantAttributes;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ScheduleUtil {
  private static final String FORCED_SCHEDULE_RELOAD_PARAM = "forceSchedulesReload";

  private ScheduleUtil() {

  }

  /**
   * For quartz scheduling (with db store) schedule configs are stored to the quartz tables when scheduled,
   * so on module upgrade schedule information is not lost and it's not needed to reload it each time.
   * Schedules will be reloaded only for:
   * 1) migration from spring in-memory version to quartz enabled version
   * 2) first time enabled tenant
   * 3) forceSchedulesReload is set to true in tenantAttributes parameters
   *
   * @param tenantAttributes        the tenant attributes containing moduleFrom and moduleTo versions
   * @param minQuartzSupportVersion module version since which quartz scheduling is enabled
   * @return whether existing schedule configs need to be migrated to quartz
   */
  public static boolean shouldMigrateSchedulesToQuartz(TenantAttributes tenantAttributes, SemVer minQuartzSupportVersion) {
    log.debug("shouldMigrateSchedulesToQuartz:: parameters tenantAttributes: {}, minQuartzSupportVersion: {}",
      tenantAttributes, minQuartzSupportVersion);

    if (Stream.ofNullable(tenantAttributes.getParameters()).flatMap(Collection::stream).anyMatch(
      p -> FORCED_SCHEDULE_RELOAD_PARAM.equalsIgnoreCase(p.getKey()) && "true".equalsIgnoreCase(p.getValue()))) {
      log.info("shouldMigrateSchedulesToQuartz:: forceSchedulesReload is set to true." +
        " Existing schedules will be loaded from the configuration");
      return true;
    }

    String moduleFrom = tenantAttributes.getModuleFrom();
    String moduleTo = tenantAttributes.getModuleTo();
    // this should be a case only for module disabling, so should not have such case at all
    if (moduleTo == null) {
      log.info("shouldMigrateSchedulesToQuartz::could not determine moduleTo version. Returning false");
      return false;
    }

    boolean shouldMigrateSchedulesToQuartz = isMigrationNeededForVersions(moduleFrom, moduleTo, minQuartzSupportVersion);
    if (shouldMigrateSchedulesToQuartz) {
      log.info("shouldMigrateSchedulesToQuartz:: upgrade from version='{}' to version='{}' requires migration " +
          "from in-memory to quartz scheduling. Existing schedules will be loaded from the configuration",
        moduleFrom, moduleTo);
    } else {
      log.info("shouldMigrateSchedulesToQuartz:: upgrade from version='{}' to version='{}' does not require " +
        "schedules migration", moduleFrom, moduleTo);
    }
    return shouldMigrateSchedulesToQuartz;
  }

  private static boolean isMigrationNeededForVersions(String moduleFrom, String moduleTo, SemVer minQuartzSupportVersion) {
    SemVer moduleFromSemVer = moduleFrom != null ? moduleVersionToSemVer(moduleFrom) : null;
    SemVer moduleToSemVer = moduleVersionToSemVer(moduleTo);

    return moduleToSemVer.compareTo(minQuartzSupportVersion) >= 0 &&
      (moduleFromSemVer == null || moduleFromSemVer.compareTo(minQuartzSupportVersion) < 0);
  }

  private static SemVer moduleVersionToSemVer(String version) {
    try {
      return new SemVer(version);
    } catch (IllegalArgumentException ex) {
      return new ModuleId(version).getSemVer();
    }
  }
}
