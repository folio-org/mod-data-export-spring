package org.folio.des.service;

import lombok.extern.log4j.Log4j2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.scheduling.bursar.BursarScheduledJobInitializer;
import org.folio.des.scheduling.quartz.OldJobDeleteScheduler;
import org.folio.des.scheduling.quartz.ScheduledJobsRemover;
import org.folio.des.service.bursarlegacy.BursarExportLegacyJobService;
import org.folio.des.service.bursarlegacy.BursarMigrationService;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.des.service.config.impl.BursarFeesFinesExportConfigService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  private static final Pattern MODULE_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
  // bursar changes were introduced during v3.2.0 development cycle
  private static final int BURSAR_UPGRADE_VERSION_THRESHOLD = 3;

  private final KafkaService kafka;
  private final BulkEditConfigService bulkEditConfigService;
  private final EdifactScheduledJobInitializer edifactScheduledJobInitializer;
  private final ScheduledJobsRemover scheduledJobsRemover;
  private final BursarScheduledJobInitializer bursarScheduledJobInitializer;
  private final OldJobDeleteScheduler oldJobDeleteScheduler;
  private final BursarExportLegacyJobService bursarExportLegacyJobService;
  private final JobService jobService;
  private final PrepareSystemUserService prepareSystemUserService;
  private final BursarMigrationService bursarMigrationService;
  private final BursarFeesFinesExportConfigService bursarFeesFinesExportConfigService;

  public FolioTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase,
      PrepareSystemUserService prepareSystemUserService, KafkaService kafka, BulkEditConfigService bulkEditConfigService,
      EdifactScheduledJobInitializer edifactScheduledJobInitializer, ScheduledJobsRemover scheduledJobsRemover,
      BursarScheduledJobInitializer bursarScheduledJobInitializer, OldJobDeleteScheduler oldJobDeleteScheduler,
      BursarExportLegacyJobService bursarExportLegacyJobService, JobService jobService,
      BursarMigrationService bursarMigrationService, BursarFeesFinesExportConfigService bursarFeesFinesExportConfigService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.prepareSystemUserService = prepareSystemUserService;
    this.kafka = kafka;
    this.bulkEditConfigService = bulkEditConfigService;
    this.edifactScheduledJobInitializer = edifactScheduledJobInitializer;
    this.scheduledJobsRemover = scheduledJobsRemover;
    this.bursarScheduledJobInitializer = bursarScheduledJobInitializer;
    this.oldJobDeleteScheduler = oldJobDeleteScheduler;
    this.bursarExportLegacyJobService = bursarExportLegacyJobService;
    this.jobService = jobService;
    this.bursarMigrationService = bursarMigrationService;
    this.bursarFeesFinesExportConfigService = bursarFeesFinesExportConfigService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      if (shouldUpdateBursar(tenantAttributes)) {
        log.info("Updating bursar jobs and configs");
        bursarMigrationService.updateLegacyBursarJobs(bursarExportLegacyJobService, jobService);
        bursarMigrationService.updateLegacyBursarConfigs(bursarFeesFinesExportConfigService);
      }

      prepareSystemUserService.setupSystemUser();
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
    oldJobDeleteScheduler.removeJobs(tenantId);
  }

  protected boolean shouldUpdateBursar(TenantAttributes tenantAttributes) {
    // fresh install
    if (tenantAttributes.getModuleFrom() == null) {
      return false;
    }

    Matcher versionMatcher = MODULE_VERSION_PATTERN.matcher(tenantAttributes.getModuleFrom());
    if (!versionMatcher.find()) {
      // if we can't extract version number, we can't compare, so let's assume we need to update
      return true;
    }

    String version = versionMatcher.group();
    int major = Integer.parseInt(version.split("\\.")[0]);
    return major <= BURSAR_UPGRADE_VERSION_THRESHOLD;
  }
}
