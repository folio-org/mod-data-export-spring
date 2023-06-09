package org.folio.des.scheduling.quartz.job.bursar;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.job.JobKeyResolver;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

@Component
public class BursarDeleteJobKeyResolver implements JobKeyResolver {

  @Override
  public JobKey resolve(ExportConfig exportConfig) {
    if (exportConfig == null) {
      return null;
    }
    String jobId = exportConfig.getId();
    return JobKey.jobKey(jobId, getJobGroup(exportConfig));
  }

  private String getJobGroup(ExportConfig exportConfig) {
    return exportConfig.getTenant() + "_" + QuartzConstants.BURSAR_EXPORT_DELETE_GROUP_NAME;
  }
}
