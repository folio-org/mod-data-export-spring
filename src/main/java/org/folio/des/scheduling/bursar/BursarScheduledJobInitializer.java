package org.folio.des.scheduling.bursar;

import java.util.Optional;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.BursarExportScheduler;
import org.folio.des.service.config.ExportConfigService;
import org.folio.okapi.common.SemVer;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static org.folio.des.scheduling.acquisition.ScheduleUtil.shouldMigrateSchedulesToQuartz;

@Component
@RequiredArgsConstructor
@Log4j2
public class BursarScheduledJobInitializer {

  private final ExportConfigService burSarExportConfigService;
  private final BursarExportScheduler bursarExportScheduler;

  private final SemVer quartzEdifactMinVersion = new SemVer("3.0.0-SNAPSHOT");

  public void initAllScheduledJob(TenantAttributes tenantAttributes) {
    log.info("initAllScheduledJob:: initiating scheduled job of type Bursar");
    try {
      Optional<ExportConfig> savedConfig = burSarExportConfigService.getFirstConfig();
      if (savedConfig.isPresent() && shouldMigrateSchedulesToQuartz(tenantAttributes,
        quartzEdifactMinVersion)) {
        bursarExportScheduler.scheduleBursarJob(savedConfig.get());
      } else {
        log.info("initAllScheduledJob:: No export schedules found.");
      }
    } catch (Exception e) {
      log.warn("scheduling failure", e);
    }
  }
}
