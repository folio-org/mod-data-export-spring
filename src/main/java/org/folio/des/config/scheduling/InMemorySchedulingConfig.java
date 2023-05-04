package org.folio.des.config.scheduling;

import org.folio.des.builder.job.EdifactOrdersJobCommandSchedulerBuilder;
import org.folio.des.builder.scheduling.EdifactScheduledTaskBuilder;
import org.folio.des.builder.scheduling.ScheduledTaskBuilder;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.converter.aqcuisition.InitEdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.scheduling.acquisition.EdifactOrdersExportJobScheduler;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import lombok.RequiredArgsConstructor;

@Configuration
@ConditionalOnProperty(prefix = "folio.quartz", name = "enabled", havingValue = "false")
@RequiredArgsConstructor
public class InMemorySchedulingConfig implements SchedulingConfig {
  private final JobService jobService;
  private final JobExecutionService jobExecutionService;
  private final EdifactOrdersJobCommandSchedulerBuilder jobSchedulerCommandBuilder;
  private final EdifactOrdersExportConfigToTaskTriggerConverter triggerConverter;
  private final InitEdifactOrdersExportConfigToTaskTriggerConverter initTriggerConverter;
  private final FolioExecutionContextHelper contextHelper;

  @Value("${folio.schedule.acquisition.runOnlyIfModuleRegistered:true}")
  private String runOnlyIfModuleRegistered;

  @Value("${folio.schedule.acquisition.poolSize:10}")
  private int poolSize;

  @Bean
  @Override
  public ExportJobScheduler edifactOrdersExportJobScheduler() {
    return new EdifactOrdersExportJobScheduler(new ThreadPoolTaskScheduler(), triggerConverter,
      edifactScheduledTaskBuilder(), poolSize);
  }

  @Bean
  @Override
  public ExportJobScheduler initEdifactOrdersExportJobScheduler() {
    return new EdifactOrdersExportJobScheduler(new ThreadPoolTaskScheduler(), initTriggerConverter,
      edifactScheduledTaskBuilder(), poolSize);
  }

  @Bean
  @Override
  public AcqSchedulingProperties acqSchedulingProperties() {
    return new AcqSchedulingProperties(runOnlyIfModuleRegistered);
  }

  @Bean
  ScheduledTaskBuilder edifactScheduledTaskBuilder() {
    return new EdifactScheduledTaskBuilder(jobService, contextHelper, acqSchedulingProperties(),
      jobExecutionService, jobSchedulerCommandBuilder);
  }
}
