package org.folio.des.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.des.scheduling.ExportScheduler;
import org.folio.des.security.AuthtService;
import org.folio.spring.controller.TenantController;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController("folioTenantController")
@RequestMapping(value = "/_/")
public class FolioTenantController extends TenantController {

  private final ExportScheduler scheduler;
  private final AuthtService authtService;

  public FolioTenantController(
      TenantService baseTenantService, ExportScheduler scheduler, AuthtService authtService) {
    super(baseTenantService);
    this.scheduler = scheduler;
    this.authtService = authtService;
  }

  @Override
  public ResponseEntity<String> postTenant(TenantAttributes tenantAttributes) {
    var tenantInit = super.postTenant(tenantAttributes);

    if (tenantInit.getStatusCode() == HttpStatus.OK) {
      authtService.authorizeModuleWithSystemUser();
      scheduler.initScheduleConfiguration();
    }

    return tenantInit;
  }
}
