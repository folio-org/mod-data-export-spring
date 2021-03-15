package org.folio.des.controller;

import static org.springframework.http.ResponseEntity.status;

import lombok.extern.log4j.Log4j2;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.scheduling.ExportScheduler;
import org.folio.des.security.SecurityManagerService;
import org.folio.spring.FolioExecutionContext;
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

  private final FolioExecutionContext context;
  private final SecurityManagerService securityManagerService;

  public FolioTenantController(TenantService baseTenantService,
    FolioExecutionContextHelper contextHelper,
    ExportScheduler scheduler, FolioExecutionContext context,
    SecurityManagerService securityManagerService) {
    super(baseTenantService);
    this.contextHelper = contextHelper;
    this.scheduler = scheduler;
    this.context = context;
    this.securityManagerService = securityManagerService;
  }

  @Override
  public ResponseEntity<String> postTenant(TenantAttributes tenantAttributes) {
    var tenantInit = super.postTenant(tenantAttributes);

    if (tenantInit.getStatusCode() == HttpStatus.OK) {
      try {
        contextHelper.storeOkapiHeaders();
        var tenantId = context.getTenantId();
        securityManagerService.prepareSystemUser(context.getOkapiUrl(), tenantId);
        scheduler.initScheduleConfiguration();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        return status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
      }
    }

    return tenantInit;
  }

}
