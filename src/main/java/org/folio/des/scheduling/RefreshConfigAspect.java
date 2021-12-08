package org.folio.des.scheduling;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.folio.des.domain.dto.ExportConfig;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RefreshConfigAspect {

  private final ExportScheduler scheduler;

  @After("(execution(* org.folio.des.service.config.ExportConfigService.updateConfig(..)) && args(..,config))")
  public void refreshAfterUpdate(ExportConfig config) {
    scheduler.updateTasks(config);
  }

  @After("(execution(* org.folio.des.service.config.ExportConfigService.postConfig(..)) && args(config))")
  public void refreshAfterPost(ExportConfig config) {
    scheduler.updateTasks(config);
  }
}
