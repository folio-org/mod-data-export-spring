package org.folio.des.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.ExportScheduler;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.service.JobService;
import org.folio.des.service.bursarlegacy.BursarExportLegacyJobService;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.des.util.LegacyBursarMigrationUtil;
import org.folio.spring.controller.TenantController;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("folioTenantController")
@RequestMapping
@Log4j2
public class FolioTenantController extends TenantController {

  private final FolioExecutionContextHelper contextHelper;
  private final ExportScheduler scheduler;
  private final KafkaService kafka;
  private final BulkEditConfigService bulkEditConfigService;
  private final EdifactScheduledJobInitializer edifactScheduledJobInitializer;

  private final BursarExportLegacyJobService bursarExportLegacyJobService;

  private final JobService jobService;

  public FolioTenantController(
    TenantService baseTenantService,
    FolioExecutionContextHelper contextHelper,
    ExportScheduler scheduler,
    KafkaService kafka,
    BulkEditConfigService bulkEditConfigService,
    EdifactScheduledJobInitializer edifactScheduledJobInitializer,
    BursarExportLegacyJobService bursarExportLegacyJobService,
    JobService jobService
  ) {
    super(baseTenantService);
    this.contextHelper = contextHelper;
    this.scheduler = scheduler;
    this.kafka = kafka;
    this.bulkEditConfigService = bulkEditConfigService;
    this.edifactScheduledJobInitializer = edifactScheduledJobInitializer;
    this.bursarExportLegacyJobService = bursarExportLegacyJobService;
    this.jobService = jobService;
  }

  @Override
  public ResponseEntity<Void> postTenant(TenantAttributes tenantAttributes) {
    var tenantInit = super.postTenant(tenantAttributes);

    if (tenantInit.getStatusCode() == HttpStatus.NO_CONTENT) {
      try {
        contextHelper.registerTenant();
        scheduler.initScheduleConfiguration();
        bulkEditConfigService.checkBulkEditConfiguration();
        edifactScheduledJobInitializer.initAllScheduledJob();
        kafka.createKafkaTopics();
        kafka.restartEventListeners();
        LegacyBursarMigrationUtil.recreateLegacyJobs(
          bursarExportLegacyJobService,
          jobService
        );
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.internalServerError().build();
      }
    }

    return tenantInit;
  }
}
