package org.folio.des.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.scheduling.ExportScheduler;
import org.folio.spring.controller.TenantController;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("folioTenantController")
@RequestMapping(value = "/_/")
@Log4j2
public class FolioTenantController extends TenantController {

  private final FolioExecutionContextHelper contextHelper;
  private final ExportScheduler scheduler;

  public FolioTenantController(TenantService baseTenantService, FolioExecutionContextHelper contextHelper,
      ExportScheduler scheduler) {
    super(baseTenantService);
    this.contextHelper = contextHelper;
    this.scheduler = scheduler;
  }

  @Override
  public ResponseEntity<String> postTenant(TenantAttributes tenantAttributes) {
    var tenantInit = super.postTenant(tenantAttributes);

    if (tenantInit.getStatusCode() == HttpStatus.OK) {
      try {
        contextHelper.storeOkapiHeaders();
        scheduler.initScheduleConfiguration();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }

    return tenantInit;
  }

}
