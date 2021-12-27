package org.folio.des.config;

import org.folio.des.builder.scheduling.EdifactScheduledTaskBuilder;
import org.folio.des.builder.scheduling.ScheduledTaskBuilder;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.scheduling.acquisition.EdifactOrdersExportJobScheduler;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.service.JobService;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfiguration {
  @Bean
  ScheduledTaskBuilder edifactScheduledTaskBuilder(JobService jobService, FolioExecutionContextHelper contextHelper,
                    AcqSchedulingProperties acqSchedulingProperties) {
    return new EdifactScheduledTaskBuilder(jobService, contextHelper, acqSchedulingProperties);
  }

  @Bean
  EdifactOrdersExportJobScheduler edifactOrdersExportJobScheduler(ScheduledTaskBuilder edifactScheduledTaskBuilder,
                    EdifactOrdersExportConfigToTaskTriggerConverter triggerConverter,
                    EdifactScheduledJobInitializer edifactScheduledJobInitializer,
                    @Value("${folio.schedule.acquisition.poolSize:10}") int poolSize) {
    return new EdifactOrdersExportJobScheduler(new ThreadPoolTaskScheduler(), triggerConverter,
                edifactScheduledTaskBuilder, poolSize, edifactScheduledJobInitializer);
  }

  @Bean
  AcqSchedulingProperties acqSchedulingProperties(
            @Value("${folio.schedule.acquisition.runOnlyIfModuleRegistered:true}") String runOnlyIfModuleRegistered) {
    return new AcqSchedulingProperties(runOnlyIfModuleRegistered);
  }

  @Bean
  EdifactScheduledJobInitializer edifactScheduledJobInitializer( BaseExportConfigService baseExportConfigService,
                    FolioExecutionContextHelper contextHelper, AcqSchedulingProperties acqSchedulingProperties) {
    return new EdifactScheduledJobInitializer(baseExportConfigService, contextHelper, acqSchedulingProperties);
  }
}
