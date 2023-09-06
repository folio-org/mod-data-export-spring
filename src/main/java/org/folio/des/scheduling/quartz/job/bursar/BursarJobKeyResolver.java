package org.folio.des.scheduling.quartz.job.bursar;

import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.job.JobKeyResolver;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class BursarJobKeyResolver implements JobKeyResolver {
  @Override
  public JobKey resolve(ExportConfig exportConfig) {
    log.debug("resolve:: input param={}", exportConfig);
    if (exportConfig == null) {
      log.debug("resolve:: input param is null. Nothing to resolve");
      return null;
    }
    String jobId = exportConfig.getId();
    return JobKey.jobKey(jobId, getJobGroup(exportConfig));
  }

  private String getJobGroup(ExportConfig exportConfig) {
    return exportConfig.getTenant() + "_" + QuartzConstants.BURSAR_EXPORT_GROUP_NAME;
  }
}
