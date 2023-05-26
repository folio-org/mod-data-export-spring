package org.folio.des.scheduling.quartz;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.exceptions.SchedulingException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class ScheduledJobsRemover {
  private final Scheduler scheduler;
  private final List<String> exportTypes;

  public void deleteJobs(String tenantId) {
    if (CollectionUtils.isNotEmpty(exportTypes)) {
      // to delete all different export type relate to tenant id.
      for (String exportType : exportTypes) {
        deleteJobGroup(tenantId, exportType);
      }
    } else {
      log.info("deleteJob:: No export types found to delete for tenant '{}'", tenantId);
    }
  }

  private void deleteJobGroup(String tenantId, String exportType) {
    String jobGroup = getJobGroup(tenantId, exportType);
    log.info("deleteJobGroup:: Trying to delete job group '{}'", jobGroup);
    try {
      Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup));
      scheduler.deleteJobs(new ArrayList<>(jobKeys));
      log.info("deleteJobGroup:: Scheduled Job Keys with size={} deleted", jobKeys.size());
    } catch (SchedulerException e) {
      log.error("deleteJobGroup:: Error during job group '{}' deletion", jobGroup, e);
      throw new SchedulingException("Error during deleting job", e);
    }
  }

  private String getJobGroup(String tenantName, String exportType) {
    return tenantName + "_" + exportType;
  }
}
