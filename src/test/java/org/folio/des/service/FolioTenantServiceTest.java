package org.folio.des.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.scheduling.bursar.BursarScheduledJobInitializer;
import org.folio.des.scheduling.quartz.OldJobDeleteScheduler;
import org.folio.des.scheduling.quartz.ScheduledJobsRemover;
import org.folio.des.service.bursarlegacy.BursarExportLegacyJobService;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.des.util.LegacyBursarMigrationUtil;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class FolioTenantServiceTest {

  @InjectMocks
  FolioTenantService folioTenantService;

  @Mock
  KafkaService kafka;
  @Mock
  BulkEditConfigService bulkEditConfigService;
  @Mock
  EdifactScheduledJobInitializer edifactScheduledJobInitializer;
  @Mock
  FolioExecutionContext folioExecutionContext;
  @Mock
  ScheduledJobsRemover scheduledJobsRemover;

  @Mock
  BursarScheduledJobInitializer bursarScheduledJobInitializer;

  @Mock
  OldJobDeleteScheduler oldJobDeleteScheduler;
  @Mock
  PrepareSystemUserService prepareSystemUserService;

  @Mock
  BursarExportLegacyJobService bursarExportLegacyJobService;

  @Test
  void shouldDoProcessAfterTenantUpdating() {
    TenantAttributes tenantAttributes = createTenantAttributes();

    doNothing().when(prepareSystemUserService).setupSystemUser();
    doNothing().when(bulkEditConfigService).checkBulkEditConfiguration();
    doNothing().when(edifactScheduledJobInitializer).initAllScheduledJob(tenantAttributes);
    doNothing().when(kafka).createKafkaTopics();
    doNothing().when(kafka).restartEventListeners();
    doNothing().when(bursarScheduledJobInitializer).initAllScheduledJob(tenantAttributes);
    doNothing().when(oldJobDeleteScheduler).scheduleOldJobDeletion(any());

    try (MockedStatic<LegacyBursarMigrationUtil> mockedBLegacyBursarMigrationUtil = mockStatic(LegacyBursarMigrationUtil.class)) {
      mockedBLegacyBursarMigrationUtil.when(() -> LegacyBursarMigrationUtil.recreateLegacyJobs(any(), any()))
        .thenAnswer((Answer<Void>) invocation -> null);

      folioTenantService.afterTenantUpdate(tenantAttributes);
    }


    verify(prepareSystemUserService).setupSystemUser();
    verify(bulkEditConfigService, times(1)).checkBulkEditConfiguration();
    verify(edifactScheduledJobInitializer, times(1)).initAllScheduledJob(tenantAttributes);
    verify(bursarScheduledJobInitializer, times(1)).initAllScheduledJob(tenantAttributes);
    verify(oldJobDeleteScheduler, times(1)).scheduleOldJobDeletion(any());
    verify(kafka, times(1)).createKafkaTopics();
    verify(kafka, times(1)).restartEventListeners();
  }

  @Test
  void shouldDeleteJob() {
    String tenantId = "tenantA";
    TenantAttributes tenantAttributes = createTenantAttributes();
    tenantAttributes.setPurge(true);

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    doNothing().when(scheduledJobsRemover).deleteJobs(tenantId);
    doNothing().when(oldJobDeleteScheduler).removeJobs(tenantId);

    folioTenantService.afterTenantDeletion(tenantAttributes);

    verify(scheduledJobsRemover, times(1)).deleteJobs(tenantId);
  }

  private TenantAttributes createTenantAttributes() {
    TenantAttributes tenantAttributes = new TenantAttributes();
    tenantAttributes.setPurge(false);
    tenantAttributes.setModuleTo("mod-data-export-spring");
    return tenantAttributes;
  }
}
