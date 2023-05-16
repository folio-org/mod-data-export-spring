package org.folio.des.scheduling.acquisition;

import static org.folio.des.scheduling.acquisition.ScheduleUtil.isJobScheduleAllowed;
import static org.folio.des.scheduling.acquisition.ScheduleUtil.shouldMigrateSchedulesToQuartz;

import java.util.ArrayList;
import java.util.List;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.Job;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.okapi.common.SemVer;
import org.folio.tenant.domain.dto.TenantAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class EdifactScheduledJobInitializer {
  private final static String ALL_EDIFACT_ORDERS_CONFIG_QUERY = "type==EDIFACT_ORDERS_EXPORT";
  private final ExportTypeBasedConfigManager basedConfigManager;
  private final FolioExecutionContextHelper contextHelper;
  private final AcqSchedulingProperties acqSchedulingProperties;
  private final ExportJobScheduler exportJobScheduler;
  private final boolean isQuartzEdifactEnabled;
  //mod-data-export-spring version in which quartz scheduling for edifact order export was enabled
  private final SemVer quartzEdifactMinVersion = new SemVer("3.0.0-SNAPSHOT");

  public void initAllScheduledJob(TenantAttributes tenantAttributes) {
    log.info("initAllScheduledJob:: initialize EDIFACT scheduled job: is module registered: {}, " +
        "tenantAttributes: {}", contextHelper.isModuleRegistered(), tenantAttributes);
    List<ExportConfig> exportConfigs = new ArrayList<>();
    try {
      boolean shouldScheduleInitialConfigs = (isQuartzEdifactEnabled && shouldMigrateSchedulesToQuartz(tenantAttributes,
        quartzEdifactMinVersion)) || (!isQuartzEdifactEnabled && isJobScheduleAllowed(
        acqSchedulingProperties.isRunOnlyIfModuleRegistered(), contextHelper.isModuleRegistered()));
      if (shouldScheduleInitialConfigs) {
        ExportConfigCollection exportConfigCol = basedConfigManager.getConfigCollection(ALL_EDIFACT_ORDERS_CONFIG_QUERY, Integer.MAX_VALUE);
        exportConfigs = exportConfigCol.getConfigs();
        for (ExportConfig exportConfig : exportConfigs) {
          List<Job> scheduledJobs = exportJobScheduler.scheduleExportJob(exportConfig);
          scheduledJobs.forEach(scheduledJob -> log.info("InitialJob scheduled: {}", scheduledJob.getId()));
        }
        log.info("initAllScheduledJob:: scheduling configurations loaded and jobs scheduled successfully");
      }
    }
    catch (Exception ex) {
      log.error("initAllScheduledJob:: Exception for initial EDIFACT scheduling with configs size: {}",
        exportConfigs.size(), ex);
    }
  }
}
