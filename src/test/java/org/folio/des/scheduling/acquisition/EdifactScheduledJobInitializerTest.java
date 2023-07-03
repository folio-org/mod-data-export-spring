package org.folio.des.scheduling.acquisition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EdifactScheduledJobInitializerTest {
  private ExportTypeBasedConfigManager exportTypeBasedConfigManager = mock(ExportTypeBasedConfigManager.class);
  private ExportJobScheduler exportJobScheduler = mock(ExportJobScheduler.class);
  private TenantAttributes tenantAttributes = new TenantAttributes().moduleTo("3.0.0");

  private EdifactScheduledJobInitializer edifactScheduledJobInitializer;

  @BeforeEach
  void before() {
    edifactScheduledJobInitializer = spy(new EdifactScheduledJobInitializer(exportTypeBasedConfigManager, exportJobScheduler));
  }

  @Test
  void shouldScheduleJobsIfUpgradeToQuartzEnabledVersion() {
    mockConfigCollection();

    edifactScheduledJobInitializer.initAllScheduledJob(tenantAttributes);

    verify(exportTypeBasedConfigManager, times(1)).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    verify(exportJobScheduler, times(1)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldSkipScheduleJobsIfUpgradeFromQuartzEnabledVersion() {
    edifactScheduledJobInitializer.initAllScheduledJob(new TenantAttributes().moduleFrom("3.0.0").moduleTo("4.0.0"));

    verify(exportTypeBasedConfigManager, times(0)).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    verify(exportJobScheduler, times(0)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldScheduleJobsIfUpgradeWithForceSchedulesReload() {
    mockConfigCollection();

    edifactScheduledJobInitializer.initAllScheduledJob(new TenantAttributes().moduleFrom("3.0.0").moduleTo("4.0.0")
      .parameters(List.of(new Parameter().key("forceSchedulesReload").value("true"))));

    verify(exportTypeBasedConfigManager, times(1)).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    verify(exportJobScheduler, times(1)).scheduleExportJob(any(ExportConfig.class));
  }

  private void mockConfigCollection() {
    ExportConfigCollection configCollection = new ExportConfigCollection();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    configCollection.addConfigsItem(exportConfig);
    doReturn(configCollection).when(exportTypeBasedConfigManager).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
  }
}
