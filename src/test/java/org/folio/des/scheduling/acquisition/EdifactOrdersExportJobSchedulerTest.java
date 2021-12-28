package org.folio.des.scheduling.acquisition;

import static org.mockito.Mockito.verify;

import org.folio.des.builder.scheduling.ScheduledTaskBuilder;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.converter.aqcuisition.EdifactOrdersExportConfigToTaskTriggerConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootTest(classes = { JacksonConfiguration.class, ServiceConfiguration.class})
public class EdifactOrdersExportJobSchedulerTest {
  @Autowired
  private EdifactOrdersExportJobScheduler scheduler;
  @MockBean
  private ThreadPoolTaskScheduler taskScheduler;
  @MockBean
  private EdifactOrdersExportConfigToTaskTriggerConverter converter;
  @MockBean
  private ScheduledTaskBuilder scheduledTaskBuilder;
  @MockBean
  private EdifactScheduledJobInitializer edifactScheduledJobInitializer;

  @AfterEach
  public void resetMocks() {
    Mockito.reset(taskScheduler, scheduledTaskBuilder);
  }

  @Test
  void shouldInvokeInitializerForScheduleAllJobOnModuleStartTime() {
    scheduler.initAllScheduledJob();
    verify(edifactScheduledJobInitializer).initAllScheduledJob(scheduler);
  }
}
