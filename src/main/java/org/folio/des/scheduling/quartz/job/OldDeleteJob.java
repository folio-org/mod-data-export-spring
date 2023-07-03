package org.folio.des.scheduling.quartz.job;

import static org.folio.des.scheduling.quartz.QuartzConstants.TENANT_ID_PARAM;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.service.JobService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class OldDeleteJob implements org.quartz.Job {

  private final JobService jobService;
  private final FolioExecutionContextHelper contextHelper;
  private static final String PARAM_NOT_FOUND_MESSAGE = "'%s' param is missing in the jobExecutionContext";

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String tenantId = getTenantId(jobExecutionContext);
    try (var context = new FolioExecutionContextSetter(contextHelper.getFolioExecutionContext(tenantId))) {
      jobService.deleteOldJobs();
    }
    log.info("execute:: deleteOldJobs executed");
  }

  private String getTenantId(JobExecutionContext jobExecutionContext) {
    String tenantId = jobExecutionContext.getJobDetail().getJobDataMap().getString(TENANT_ID_PARAM);
    if (tenantId == null) {
      throw new IllegalArgumentException(String.format(PARAM_NOT_FOUND_MESSAGE, TENANT_ID_PARAM));
    }
    return tenantId;
  }
}
