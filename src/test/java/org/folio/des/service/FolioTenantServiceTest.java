package org.folio.des.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.des.config.kafka.KafkaService;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.scheduling.bursar.BursarScheduledJobInitializer;
import org.folio.des.scheduling.quartz.OldJobDeleteScheduler;
import org.folio.des.scheduling.quartz.ScheduledJobsRemover;
import org.folio.des.service.bursarlegacy.BursarMigrationService;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  BursarMigrationService bursarMigrationService;

  @Test
  void shouldDoProcessAfterTenantUpdating() {
    TenantAttributes tenantAttributes = createTenantAttributes();

    doNothing().when(prepareSystemUserService)
      .setupSystemUser();
    doNothing().when(bulkEditConfigService)
      .checkBulkEditConfiguration();
    doNothing().when(edifactScheduledJobInitializer)
      .initAllScheduledJob(tenantAttributes);
    doNothing().when(kafka)
      .createKafkaTopics();
    doNothing().when(kafka)
      .restartEventListeners();
    doNothing().when(bursarScheduledJobInitializer)
      .initAllScheduledJob(tenantAttributes);
    doNothing().when(oldJobDeleteScheduler)
      .scheduleOldJobDeletion(any());
    doNothing().when(bursarMigrationService)
      .updateLegacyBursarJobs(any(), any());

    folioTenantService.afterTenantUpdate(tenantAttributes);

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
    doNothing().when(scheduledJobsRemover)
      .deleteJobs(tenantId);
    doNothing().when(oldJobDeleteScheduler)
      .removeJobs(tenantId);

    folioTenantService.afterTenantDeletion(tenantAttributes);

    verify(scheduledJobsRemover, times(1)).deleteJobs(tenantId);
  }

  private TenantAttributes createTenantAttributes() {
    TenantAttributes tenantAttributes = new TenantAttributes();
    tenantAttributes.setPurge(false);
    tenantAttributes.setModuleTo("mod-data-export-spring");
    return tenantAttributes;
  }

  @ParameterizedTest
  @CsvSource(value = {
      // new install => no need to upgrade
      ",false", // empty string is null (JUnit does this for csv source)

      // no/invalid version specified => assume need to upgrade
      "mod-data-export-spring,true", "1.0,true", "'',true",

      // newer than v3.x.x => no need to upgrade
      "mod-data-export-spring-4.0.0,false", "mod-data-export-spring-4.0.0-SNAPSHOT,false",
      "mod-data-export-spring-4.0.0.1abcdef,false", // Git revisions are sometimes used
      "mod-data-export-spring-999.0.0,false",

      // v3.x.x => need to upgrade
      "mod-data-export-spring-3.0.0,true", "mod-data-export-spring-3.999.0,true", "mod-data-export-spring-3.0.0-SNAPSHOT,true",
      "mod-data-export-spring-3.0.0.1abcdef,true",

      // very old
      "mod-data-export-spring-1.0.0,true", "mod-data-export-spring-1.0.0-SNAPSHOT,true",
      "mod-data-export-spring-1.0.0.1abcdef,true", "mod-data-export-spring-0.0.0,true", })
  void testShouldUpdateBursar(String moduleFrom, boolean expected) {
    TenantAttributes tenantAttributes = new TenantAttributes().moduleFrom(moduleFrom);
    assertEquals(expected, folioTenantService.shouldUpdateBursar(tenantAttributes), moduleFrom + "=" + expected);
  }
}
