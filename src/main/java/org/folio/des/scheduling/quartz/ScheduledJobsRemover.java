package org.folio.des.scheduling.quartz;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.exceptions.SchedulingException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

@Log4j2
@RequiredArgsConstructor
public class ScheduledJobsRemover {
  private final Scheduler scheduler;
  private List<String> exportTypes;

  public ScheduledJobsRemover(List<String> exportTypes, Scheduler scheduler) {
    this.exportTypes = exportTypes;
    this.scheduler = scheduler;
  }

  public void deleteJob(String tenantId) {
    // to delete all different export type relate to tenant id.
    for (String exportType : this.exportTypes) {
      deleteJobGroup(tenantId, exportType);
    }
  }

  private void deleteJobGroup(String tenantId, String exportType) {
    String ediJobGroup = getJobGroup(tenantId, exportType);
    log.info("deleteJobGroup:: Trying to delete job group '{}'", ediJobGroup);
    try {
      Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(ediJobGroup));
      scheduler.deleteJobs(new ArrayList<>(jobKeys));
      log.info("deleteJobGroup:: Scheduled Job Keys with size={} deleted", jobKeys.size());
    } catch (SchedulerException e) {
      log.error("Error during job group '{}' deletion", ediJobGroup, e);
      throw new SchedulingException("Error during deleting job", e);
    }
  }

  private String getJobGroup(String tenantName, String exportType) {
    return tenantName + "_" + exportType;
  }
}
