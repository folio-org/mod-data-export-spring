package org.folio.des.scheduling.bursar;

import static org.folio.des.scheduling.util.ScheduleUtil.shouldMigrateSchedulesToQuartz;

import java.util.Optional;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.service.config.ExportConfigService;
import org.folio.okapi.common.SemVer;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class BursarScheduledJobInitializer {

  private final ExportConfigService bursarExportConfigService;
  private final BursarExportScheduler bursarExportScheduler;


  private final SemVer quartzBursarMinVersion = new SemVer("3.0.0-SNAPSHOT");

  public void initAllScheduledJob(TenantAttributes tenantAttributes) {
    log.info("initAllScheduledJob:: initiating scheduled job of type Bursar");
    try {
      if (shouldMigrateSchedulesToQuartz(tenantAttributes, quartzBursarMinVersion)) {
        Optional<ExportConfig> savedConfig = bursarExportConfigService.getFirstConfig();
        if (savedConfig.isPresent()) {
          bursarExportScheduler.scheduleBursarJob(savedConfig.get());
        } else {
          log.info("initAllScheduledJob:: No export schedules found.");
        }
      }
    } catch (Exception e) {
      log.warn("scheduling failure", e);
    }
  }
}
