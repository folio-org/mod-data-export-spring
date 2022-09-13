package org.folio.des.scheduling.acquisition;

import static org.folio.des.scheduling.acquisition.ScheduleUtil.isJobScheduleAllowed;

import java.util.ArrayList;
import java.util.List;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class EdifactScheduledJobInitializer {
  private final static String ALL_EDIFACT_ORDERS_CONFIG_QUERY = "type==EDIFACT_ORDERS_EXPORT";
  private final ExportTypeBasedConfigManager basedConfigManager;
  private final FolioExecutionContextHelper contextHelper;
  private final AcqSchedulingProperties acqSchedulingProperties;
  private final EdifactOrdersExportJobScheduler exportJobScheduler;

  public void initAllScheduledJob() {
    log.info("Initialize EDIFACT scheduled job: is module registered: {} ", contextHelper.isModuleRegistered());
    List<ExportConfig> exportConfigs = new ArrayList<>();
    try {
      boolean isJobScheduleAllowed = isJobScheduleAllowed(acqSchedulingProperties.isRunOnlyIfModuleRegistered(),
                                                          contextHelper.isModuleRegistered());
      //if (isJobScheduleAllowed) {
        ExportConfigCollection exportConfigCol = basedConfigManager.getConfigCollection(ALL_EDIFACT_ORDERS_CONFIG_QUERY, Integer.MAX_VALUE);
        exportConfigs = exportConfigCol.getConfigs();
        for (ExportConfig exportConfig : exportConfigs) {
          List<Job> scheduledJobs = exportJobScheduler.scheduleExportJob(exportConfig);
          scheduledJobs.forEach(scheduledJob -> log.info("InitialJob scheduled: {}", scheduledJob.getId()));
        }
      //}
    }
    catch (Exception exception) {
      log.error("Exception for initial EDIFACT scheduling : " + exportConfigs.size());
    }
  }
}
