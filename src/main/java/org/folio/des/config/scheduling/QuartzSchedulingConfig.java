package org.folio.des.config.scheduling;

import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.acquisition.AcqSchedulingProperties;
import org.folio.des.scheduling.quartz.QuartzExportJobScheduler;
import org.folio.des.scheduling.quartz.converter.acquisition.ExportConfigToEdifactJobDetailConverter;
import org.folio.des.scheduling.quartz.converter.acquisition.ExportConfigToEdifactTriggerConverter;
import org.folio.des.scheduling.quartz.job.acquisition.EdifactJobKeyResolver;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@ConditionalOnProperty(prefix = "folio.quartz", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class QuartzSchedulingConfig implements SchedulingConfig {
  private final Scheduler scheduler;
  private final ExportConfigToEdifactTriggerConverter edifactTriggerConverter;
  private final ExportConfigToEdifactJobDetailConverter edifactJobDetailConverter;
  private final EdifactJobKeyResolver edifactJobKeyResolver;

  @Bean
  @Override
  public ExportJobScheduler edifactOrdersExportJobScheduler() {
    return new QuartzExportJobScheduler(scheduler, edifactTriggerConverter, edifactJobDetailConverter,
      edifactJobKeyResolver);
  }

  @Override
  public ExportJobScheduler initEdifactOrdersExportJobScheduler() {
    return edifactOrdersExportJobScheduler();
  }

  @Bean
  @Override
  public AcqSchedulingProperties acqSchedulingProperties() {
    return new AcqSchedulingProperties(false);
  }
}
