package org.folio.des.service;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.ExportScheduler;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;

@ExtendWith(MockitoExtension.class)
class FolioTenantServiceTest {

  @InjectMocks
  FolioTenantService folioTenantService;

  @Mock
  FolioExecutionContextHelper contextHelper;
  @Mock
  ExportScheduler scheduler;
  @Mock
  KafkaService kafka;
  @Mock
  BulkEditConfigService bulkEditConfigService;
  @Mock
  EdifactScheduledJobInitializer edifactScheduledJobInitializer;
  @Mock
  ExportJobScheduler exportJobScheduler;
  @Mock
  FolioExecutionContext folioExecutionContext;


  @Test
  void shouldDoProcessAfterTenantUpdating() throws Exception {
    TenantAttributes tenantAttributes = createTenantAttributes();

    doNothing().when(contextHelper).registerTenant();
    doNothing().when(scheduler).initScheduleConfiguration();
    doNothing().when(bulkEditConfigService).checkBulkEditConfiguration();
    doNothing().when(edifactScheduledJobInitializer).initAllScheduledJob(tenantAttributes);
    doNothing().when(kafka).createKafkaTopics();
    doNothing().when(kafka).restartEventListeners();

    folioTenantService.afterTenantUpdate(tenantAttributes);

    verify(contextHelper, times(1)).registerTenant();
    verify(scheduler, times(1)).initScheduleConfiguration();
    verify(bulkEditConfigService, times(1)).checkBulkEditConfiguration();
    verify(edifactScheduledJobInitializer, times(1)).initAllScheduledJob(tenantAttributes);
    verify(kafka, times(1)).createKafkaTopics();
    verify(kafka, times(1)).restartEventListeners();
  }

  @Test
  void shouldDeleteJobGroup() throws Exception {
    String tenantId = "tenant1";
    TenantAttributes tenantAttributes = createTenantAttributes();
    tenantAttributes.setPurge(true);

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    doNothing().when(exportJobScheduler).deleteJobGroup(tenantId);

    folioTenantService.afterTenantDeletion(tenantAttributes);

    verify(exportJobScheduler, times(1)).deleteJobGroup(tenantId);
  }


  @Test
  void shouldThrowSchedulerExceptionWhenDeletingJobs() throws Exception {
    String tenantId = "tenant1";
    TenantAttributes tenantAttributes = createTenantAttributes();
    tenantAttributes.setPurge(true);

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    doThrow(SchedulerException.class).when(exportJobScheduler).deleteJobGroup(tenantId);

    Assertions.assertThrows(org.folio.des.exceptions.SchedulingException.class,
      () -> folioTenantService.afterTenantDeletion(tenantAttributes));
  }

  private TenantAttributes createTenantAttributes() {
    TenantAttributes tenantAttributes = new TenantAttributes();
    tenantAttributes.setPurge(false);
    tenantAttributes.setModuleTo("mod-data-export-spring");
    return tenantAttributes;
  }
}
