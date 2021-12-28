package org.folio.des.scheduling.acquisition;

import java.util.ArrayList;
import java.util.List;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.Job;
import org.folio.des.service.config.impl.BaseExportConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static org.folio.des.scheduling.acquisition.ScheduleUtil.isJobScheduleAllowed;

@Log4j2
@RequiredArgsConstructor
public class EdifactScheduledJobInitializer {
  private final static String ALL_EDIFACT_ORDERS_CONFIG_QUERY = "query=type==EDIFACT_ORDERS_EXPORT";
  private final BaseExportConfigService baseExportConfigService;
  private final FolioExecutionContextHelper contextHelper;
  private final AcqSchedulingProperties acqSchedulingProperties;

  public void initAllScheduledJob(EdifactOrdersExportJobScheduler exportJobScheduler) {
    log.info("Initialize EDIFACT scheduled job: is module registered: {} ", contextHelper.isModuleRegistered());
    contextHelper.initScope();
    List<ExportConfig> exportConfigs = new ArrayList<>();
    try {
      boolean isJobScheduleAllowed = isJobScheduleAllowed(acqSchedulingProperties.isRunOnlyIfModuleRegistered(),
                                                          contextHelper.isModuleRegistered());
      if (isJobScheduleAllowed) {
        contextHelper.initScope();
        ExportConfigCollection exportConfigCol = baseExportConfigService.getConfigCollection(ALL_EDIFACT_ORDERS_CONFIG_QUERY);
        exportConfigs = exportConfigCol.getConfigs();
        for (ExportConfig exportConfig : exportConfigs) {
          List<Job> scheduledJobs = exportJobScheduler.scheduleExportJob(exportConfig);
          scheduledJobs.forEach(scheduledJob -> log.info("InitialJob scheduled: {}", scheduledJob.getId()));
        }
      }
    } catch (Exception exception) {
      log.error("Exception for initial EDIFACT scheduling : " + exportConfigs.size());
    }
  }
}
