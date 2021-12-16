package org.folio.des.service.config.impl;

import java.util.Optional;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.scheduling.ExportScheduler;
import org.folio.des.service.config.ExportConfigService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class ScheduledJobInitializer {
  private final ExportScheduler scheduler;
  private final ExportConfigServiceResolver exportConfigServiceResolver;

  public void initScheduleConfiguration() {
    log.info("Starting initialize scheduled Jobs");
    for (ExportType exportType : ExportType.values()) {
      Optional<ExportConfigService> exportConfigService = exportConfigServiceResolver.resolve(exportType);
      if (exportConfigService.isPresent()) {
        Optional<ExportConfig> savedConfig = exportConfigService.get().getFirstConfig();
        if (savedConfig.isPresent()) {
          log.info("Got {}.", savedConfig.get());
          scheduler.updateTasks(savedConfig.get());
        } else {
          log.info("No export schedules found for type : " + exportType);
        }
      }
    }
    log.info("Finishing Initialize scheduled Jobs");
  }
}
