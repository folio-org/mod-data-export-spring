package org.folio.des.service;

import lombok.extern.log4j.Log4j2;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.ExportScheduler;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  private final FolioExecutionContextHelper contextHelper;
  private final ExportScheduler scheduler;
  private final KafkaService kafka;
  private final BulkEditConfigService bulkEditConfigService;
  private final EdifactScheduledJobInitializer edifactScheduledJobInitializer;
  private final ExportJobScheduler exportJobScheduler;

  public FolioTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase,
                            FolioExecutionContextHelper contextHelper, ExportScheduler scheduler, KafkaService kafka,
                            BulkEditConfigService bulkEditConfigService, EdifactScheduledJobInitializer edifactScheduledJobInitializer,
                            ExportJobScheduler exportJobScheduler) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.contextHelper = contextHelper;
    this.scheduler = scheduler;
    this.kafka = kafka;
    this.bulkEditConfigService = bulkEditConfigService;
    this.edifactScheduledJobInitializer = edifactScheduledJobInitializer;
    this.exportJobScheduler = exportJobScheduler;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      contextHelper.registerTenant();
      scheduler.initScheduleConfiguration();
      bulkEditConfigService.checkBulkEditConfiguration();
      edifactScheduledJobInitializer.initAllScheduledJob();
      kafka.createKafkaTopics();
      kafka.restartEventListeners();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  @Override
  protected void afterTenantDeletion(TenantAttributes tenantAttributes) {
    try {
      String tenantId = context.getTenantId();
      exportJobScheduler.deleteJobGroup(tenantId);
    } catch (SchedulerException e) {
      log.error(e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }
}
