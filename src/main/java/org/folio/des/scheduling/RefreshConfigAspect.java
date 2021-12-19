package org.folio.des.scheduling;

import static org.folio.des.domain.dto.ExportType.BURSAR_FEES_FINES;

import java.util.EnumSet;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class RefreshConfigAspect {
  private final EnumSet<ExportType> applyAspectExportTypes = EnumSet.of(BURSAR_FEES_FINES);
  private final ExportScheduler scheduler;

  @After("(execution(* org.folio.des.service.config.ExportConfigService.updateConfig(..)) && args(..,config))")
  public void refreshAfterUpdate(ExportConfig config) {
    if (applyAspectExportTypes.contains(config.getType())) {
      scheduler.updateTasks(config);
    }
  }

  @After("(execution(* org.folio.des.service.config.ExportConfigService.postConfig(..)) && args(config))")
  public void refreshAfterPost(ExportConfig config) {
    if (applyAspectExportTypes.contains(config.getType())) {
      scheduler.updateTasks(config);
    }
  }
}
