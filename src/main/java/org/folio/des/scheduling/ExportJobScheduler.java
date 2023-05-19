package org.folio.des.scheduling;

import java.util.List;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.quartz.SchedulerException;

public interface ExportJobScheduler {
  /**
   * Schedule export job based on export config
   *
   * @param exportConfig export config
   * @return list of jobs
   */
  List<Job> scheduleExportJob(ExportConfig exportConfig);

  /**
   * Delete Jobs based on tenant name
   *
   * @param tenantId id of tenant in current context
   * @throws SchedulerException if error occurs during job deletion
   */
  void deleteJobGroup(String tenantId) throws SchedulerException;
}
