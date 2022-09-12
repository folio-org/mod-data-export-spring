package org.folio.des.builder.scheduling;

import java.util.Date;
import java.util.Optional;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.folio.des.service.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.spring.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.validation.constraints.NotNull;

@Log4j2
@RequiredArgsConstructor
public class BaseScheduledTaskBuilder implements ScheduledTaskBuilder {
  protected final JobService jobService;
  protected final FolioExecutionContextHelper contextHelper;
  @Autowired
  @Lazy
  private ExportTypeBasedConfigManager manager;
  public static final String INTEGRATION_NOT_AVAILABLE = "Integration not available";


  @Override
  public Optional<ScheduledTask> buildTask(ExportConfig exportConfig) {
    return createScheduledJob(exportConfig).map(job -> new ScheduledTask(buildRunnableTask(job), job));
  }

  @NotNull
  protected Runnable buildRunnableTask(Job job) {
    return () -> {
      var current = new Date();
      log.info("configureTasks attempt to execute at: {}: is module registered: {} ", current, contextHelper.isModuleRegistered());
      if (contextHelper.isModuleRegistered()) {
        contextHelper.initScope(job.getTenant());

        String exportConfigId = job.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getExportConfigId().toString();
   try {
      log.info("Looking config with id {}", exportConfigId);
      manager.getConfigById(exportConfigId);
    }
    catch (NotFoundException e) {
      log.info("config not found", exportConfigId);
      throw new NotFoundException(String.format(INTEGRATION_NOT_AVAILABLE, exportConfigId));
    }
        Job resultJob = jobService.upsertAndSendToKafka(job, true);
        log.info("configureTasks executed for jobId: {} at: {}", resultJob.getId(), current);
        contextHelper.finishContext();
      }
    };
  }

  protected Optional<Job> createScheduledJob(ExportConfig exportConfig) {
    Job scheduledJob;
    if (exportConfig == null) {
      return Optional.empty();
    } else {
      scheduledJob = new Job();
      scheduledJob.setType(exportConfig.getType());
      scheduledJob.setIsSystemSource(true);
      scheduledJob.setExportTypeSpecificParameters(exportConfig.getExportTypeSpecificParameters());
      scheduledJob.setTenant(exportConfig.getTenant());
      log.info("Scheduled job assigned {}.", scheduledJob);
      return Optional.of(scheduledJob);
    }
  }
}
