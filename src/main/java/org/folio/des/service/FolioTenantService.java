package org.folio.des.service;

import java.util.Map;

import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.scheduling.bursar.BursarScheduledJobInitializer;
import org.folio.des.scheduling.quartz.OldJobDeleteScheduler;
import org.folio.des.scheduling.quartz.ScheduledJobsRemover;
import org.folio.des.service.bursarlegacy.BursarExportLegacyJobService;
import org.folio.des.service.bursarlegacy.BursarMigrationService;
import org.folio.des.service.config.impl.BursarFeesFinesExportConfigService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  private static final String TENANT_NAME_PARAMETER = "tenantname";

  private final KafkaService kafka;
  private final EdifactScheduledJobInitializer edifactScheduledJobInitializer;
  private final ScheduledJobsRemover scheduledJobsRemover;
  private final BursarScheduledJobInitializer bursarScheduledJobInitializer;
  private final OldJobDeleteScheduler oldJobDeleteScheduler;
  private final BursarExportLegacyJobService bursarExportLegacyJobService;
  private final JobService jobService;
  private final BursarMigrationService bursarMigrationService;
  private final BursarFeesFinesExportConfigService bursarFeesFinesExportConfigService;
  private final FolioExecutionContext folioExecutionContext;

  public FolioTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase,
                            KafkaService kafka,
                            EdifactScheduledJobInitializer edifactScheduledJobInitializer, ScheduledJobsRemover scheduledJobsRemover,
                            BursarScheduledJobInitializer bursarScheduledJobInitializer, OldJobDeleteScheduler oldJobDeleteScheduler,
                            BursarExportLegacyJobService bursarExportLegacyJobService, JobService jobService,
                            BursarMigrationService bursarMigrationService, BursarFeesFinesExportConfigService bursarFeesFinesExportConfigService,
                            FolioExecutionContext folioExecutionContext) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.kafka = kafka;
    this.edifactScheduledJobInitializer = edifactScheduledJobInitializer;
    this.scheduledJobsRemover = scheduledJobsRemover;
    this.bursarScheduledJobInitializer = bursarScheduledJobInitializer;
    this.oldJobDeleteScheduler = oldJobDeleteScheduler;
    this.bursarExportLegacyJobService = bursarExportLegacyJobService;
    this.jobService = jobService;
    this.bursarMigrationService = bursarMigrationService;
    this.bursarFeesFinesExportConfigService = bursarFeesFinesExportConfigService;
    this.folioExecutionContext = folioExecutionContext;
  }

  @Override
  protected void beforeLiquibaseUpdate(TenantAttributes tenantAttributes) {
    var params = Map.of(TENANT_NAME_PARAMETER, folioExecutionContext.getTenantId());
    folioSpringLiquibase.setChangeLogParameters(params);
    log.info("Set ChangeLog parameters: {}", params);
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      bursarMigrationService.updateLegacyBursarIfNeeded(tenantAttributes, bursarFeesFinesExportConfigService,
          bursarExportLegacyJobService, jobService);
      bursarScheduledJobInitializer.initAllScheduledJob(tenantAttributes);
      edifactScheduledJobInitializer.initAllScheduledJob(tenantAttributes);
      oldJobDeleteScheduler.scheduleOldJobDeletion(context.getTenantId());
      kafka.createKafkaTopics();
      kafka.restartEventListeners();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  @Override
  protected void afterTenantDeletion(TenantAttributes tenantAttributes) {
    String tenantId = context.getTenantId();
    scheduledJobsRemover.deleteJobs(tenantId);
    oldJobDeleteScheduler.removeJobs(tenantId);
  }
}
