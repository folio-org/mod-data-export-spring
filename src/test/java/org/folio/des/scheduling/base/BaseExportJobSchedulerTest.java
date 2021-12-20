package org.folio.des.scheduling.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.des.builder.scheduling.ScheduledTaskBuilder;
import org.folio.des.converter.DefaultExportConfigToTaskTriggersConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.scheduling.ScheduledTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class BaseExportJobSchedulerTest {
  private ThreadPoolTaskScheduler taskScheduler = mock(ThreadPoolTaskScheduler.class);
  private Converter<ExportConfig, List<ExportTaskTrigger>> converter = new DefaultExportConfigToTaskTriggersConverter();
  private ScheduledTaskBuilder scheduledTaskBuilder = mock(ScheduledTaskBuilder.class);
  private BaseExportJobScheduler scheduler = new BaseExportJobScheduler(taskScheduler, converter, scheduledTaskBuilder, 10);

  @AfterEach
  public void resetMocks() {
    Mockito.reset(taskScheduler, scheduledTaskBuilder);
  }

  @Test
  void shouldNotScheduleJobIfExportConfigIsEmpty() {
    List<Job> scheduledJobs = scheduler.scheduleExportJob(null);
    assertTrue(scheduledJobs.isEmpty());
  }

  @Test
  void shouldNotScheduleJobIfExportConfigIsConvertedToEmptyListOfScheduledTask() {
    String expId = UUID.randomUUID().toString();
    ExportConfig ediConfig = new ExportConfig();
    ediConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);
    ediConfig.setId(expId);

    //Action
    List<Job> jobs = scheduler.scheduleExportJob(ediConfig);
    assertEquals(0, jobs.size());
    verify(taskScheduler, times(0)).schedule(any(), any(ExportTaskTrigger.class));
    verify(scheduledTaskBuilder, times(1)).buildTask(ediConfig);
  }
  @Test
  void shouldScheduleJobIfExportConfigHasInformationAboutScheduling() {
    String expId = UUID.randomUUID().toString();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(expId);
    exportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    exportConfig.setScheduleTime("15:08:39.278+00:00");
    exportConfig.setScheduleFrequency(7);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);

    Optional<ScheduledTask> scheduledTask = Optional.of(new ScheduledTask(() -> System.out.println("Job test"), new Job()));
    doReturn(scheduledTask).when(scheduledTaskBuilder).buildTask(exportConfig);

    List<Job> jobs = scheduler.scheduleExportJob(exportConfig);
    assertEquals(1, jobs.size());

    verify(taskScheduler, times(1)).schedule(any(), any(ExportTaskTrigger.class));
    verify(scheduledTaskBuilder, times(1)).buildTask(exportConfig);
  }

  @Test
  void shouldRescheduleJobIfItJobWasEarlyScheduledAndScheduledParametersIsNotChanged() {
    String expId = UUID.randomUUID().toString();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(expId);
    exportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    exportConfig.setScheduleTime("15:08:39.278+00:00");
    exportConfig.setScheduleFrequency(7);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);

    Optional<ScheduledTask> scheduledTask = Optional.of(new ScheduledTask(() -> System.out.println("Job test"), new Job()));
    doReturn(scheduledTask).when(scheduledTaskBuilder).buildTask(exportConfig);
    //Schedule
    List<Job> jobs = scheduler.scheduleExportJob(exportConfig);
    assertEquals(1, scheduler.getScheduledTasks().keySet().size());
    ExportTaskTrigger exportTaskTrigger = scheduler.getScheduledTasks().keySet().stream().findFirst().get();
    //Reschedule
    scheduler.scheduleExportJob(exportConfig);
    assertEquals(1, scheduler.getScheduledTasks().keySet().size());
    ExportTaskTrigger rescheduleExportTaskTrigger = scheduler.getScheduledTasks().keySet().stream().findFirst().get();
    assertEquals(exportTaskTrigger.getScheduleParameters(), rescheduleExportTaskTrigger.getScheduleParameters());
    assertEquals(exportTaskTrigger.getScheduleParameters().getScheduleFrequency(), rescheduleExportTaskTrigger.getScheduleParameters().getScheduleFrequency());
    verify(taskScheduler, times(1)).schedule(any(), any(ExportTaskTrigger.class));
    verify(scheduledTaskBuilder, times(1)).buildTask(exportConfig);
  }

  @Test
  void shouldRescheduleJobIfItJobWasEarlyScheduledAndScheduledParametersIsChanged() {
    String expId = UUID.randomUUID().toString();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(expId);
    exportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    exportConfig.setScheduleTime("15:08:39.278+00:00");
    exportConfig.setScheduleFrequency(7);
    exportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);

    ExportConfig reScheduledExportConfig = new ExportConfig();
    reScheduledExportConfig.setId(expId);
    reScheduledExportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    reScheduledExportConfig.setScheduleTime("15:08:39.278+00:00");
    reScheduledExportConfig.setScheduleFrequency(3);
    reScheduledExportConfig.setSchedulePeriod(ExportConfig.SchedulePeriodEnum.WEEK);

    Optional<ScheduledTask> scheduledTask = Optional.of(new ScheduledTask(() -> System.out.println("Job test"), new Job()));
    doReturn(scheduledTask).when(scheduledTaskBuilder).buildTask(exportConfig);
    Optional<ScheduledTask> resScheduledTask = Optional.of(new ScheduledTask(() -> System.out.println("Job test"), new Job()));
    doReturn(resScheduledTask).when(scheduledTaskBuilder).buildTask(reScheduledExportConfig);

    //Schedule
    List<Job> jobs = scheduler.scheduleExportJob(exportConfig);
    assertEquals(1, scheduler.getScheduledTasks().keySet().size());
    ExportTaskTrigger exportTaskTrigger = scheduler.getScheduledTasks().keySet().stream().findFirst().get();
    //Reschedule
    scheduler.scheduleExportJob(reScheduledExportConfig);
    assertEquals(1, scheduler.getScheduledTasks().keySet().size());
    ExportTaskTrigger rescheduleExportTaskTrigger = scheduler.getScheduledTasks().keySet().stream().findFirst().get();
    assertNotEquals(exportTaskTrigger.getScheduleParameters(), rescheduleExportTaskTrigger.getScheduleParameters());
    assertEquals(3, rescheduleExportTaskTrigger.getScheduleParameters().getScheduleFrequency());
    verify(taskScheduler, times(2)).schedule(any(), any(ExportTaskTrigger.class));
    verify(scheduledTaskBuilder, times(1)).buildTask(exportConfig);
    verify(scheduledTaskBuilder, times(1)).buildTask(reScheduledExportConfig);
  }

  @Test
  void testDestroy() {
    scheduler.destroy();
    assertTrue(scheduler.scheduledTasks.isEmpty());
  }

  @Test
  void shouldThrowUnsupportedOperationExceptionIfInvokeInitAllScheduledJob() {
    assertThrows(UnsupportedOperationException.class, () -> scheduler.initAllScheduledJob());
  }
}
