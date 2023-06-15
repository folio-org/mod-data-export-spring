package org.folio.des.service;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.scheduling.bursar.BursarScheduledJobInitializer;
import org.folio.des.scheduling.quartz.OldJobDeleteScheduler;
import org.folio.des.scheduling.quartz.ScheduledJobsRemover;
import org.folio.des.service.config.BulkEditConfigService;
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

  private final FolioExecutionContextHelper contextHelper;
  private final KafkaService kafka;
  private final BulkEditConfigService bulkEditConfigService;
  private final EdifactScheduledJobInitializer edifactScheduledJobInitializer;
  private final ScheduledJobsRemover scheduledJobsRemover;
  private final BursarScheduledJobInitializer bursarScheduledJobInitializer;
  private final OldJobDeleteScheduler oldJobDeleteScheduler;

  public FolioTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase,
                            FolioExecutionContextHelper contextHelper, KafkaService kafka,
                            BulkEditConfigService bulkEditConfigService, EdifactScheduledJobInitializer edifactScheduledJobInitializer,
                            ScheduledJobsRemover scheduledJobsRemover, BursarScheduledJobInitializer bursarScheduledJobInitializer,
                            OldJobDeleteScheduler oldJobDeleteScheduler) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.contextHelper = contextHelper;
    this.kafka = kafka;
    this.bulkEditConfigService = bulkEditConfigService;
    this.edifactScheduledJobInitializer = edifactScheduledJobInitializer;
    this.scheduledJobsRemover = scheduledJobsRemover;
    this.bursarScheduledJobInitializer = bursarScheduledJobInitializer;
    this.oldJobDeleteScheduler = oldJobDeleteScheduler;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      contextHelper.registerTenant();
      bursarScheduledJobInitializer.initAllScheduledJob(tenantAttributes);
      bulkEditConfigService.checkBulkEditConfiguration();
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
    oldJobDeleteScheduler.removeOldJobDeletionScheduler(tenantId);
  }
}
