package org.folio.des.scheduling.acquisition;

import java.util.List;

import org.folio.des.builder.scheduling.ScheduledTaskBuilder;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.base.BaseExportJobScheduler;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EdifactOrdersExportJobScheduler extends BaseExportJobScheduler {
  private final EdifactScheduledJobInitializer edifactScheduledJobInitializer;
  public EdifactOrdersExportJobScheduler(ThreadPoolTaskScheduler taskScheduler,
                                         Converter<ExportConfig, List<ExportTaskTrigger>> triggerConverter,
                                         ScheduledTaskBuilder scheduledTaskBuilder, int poolSize,
                                         EdifactScheduledJobInitializer edifactScheduledJobInitializer) {
    super(taskScheduler, triggerConverter, scheduledTaskBuilder, poolSize);
    this.edifactScheduledJobInitializer = edifactScheduledJobInitializer;
  }

  @Override
  public void initAllScheduledJob() {
    edifactScheduledJobInitializer.initAllScheduledJob(this);
  }


}
