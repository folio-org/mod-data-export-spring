package org.folio.des.scheduling.bursar;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.BursarExportScheduler;
import org.folio.des.service.config.ExportConfigService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Log4j2
public class BursarScheduledJobInitializer {

  private final ExportConfigService burSarExportConfigService;
  private final BursarExportScheduler bursarExportScheduler;

  public void initAllScheduledJob() {
    log.info("initiating scheduled job of type Bursar");
    try{
      Optional<ExportConfig> savedConfig = burSarExportConfigService.getFirstConfig();
      if (savedConfig.isPresent()) {
        bursarExportScheduler.scheduleBursarJob(savedConfig.get());
      }
      else {
        log.info("No export schedules found.");
      }
    }
    catch (Exception e) {
      log.error("get configuration failed for type BURSAR");
    }
  }
}
