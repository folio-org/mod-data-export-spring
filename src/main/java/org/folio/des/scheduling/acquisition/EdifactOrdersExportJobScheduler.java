package org.folio.des.scheduling.acquisition;

import java.util.List;

import org.folio.des.builder.scheduling.ScheduledTaskBuilder;
import org.folio.des.domain.dto.ExportConfig;

import org.folio.des.scheduling.base.BaseExportJobScheduler;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class EdifactOrdersExportJobScheduler extends BaseExportJobScheduler {

  public EdifactOrdersExportJobScheduler(ThreadPoolTaskScheduler taskScheduler,
                                         Converter<ExportConfig, List<ExportTaskTrigger>> triggerConverter,
                                         ScheduledTaskBuilder scheduledTaskBuilder, int poolSize) {
    super(taskScheduler, triggerConverter, scheduledTaskBuilder, poolSize);
  }

  @Override
 // @Async
  //TODO Will be implemented
  public void initAllScheduledJob() {

  }
}
