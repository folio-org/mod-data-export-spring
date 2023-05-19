package org.folio.des.scheduling.acquisition;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EdifactScheduledJobInitializerTest {
  private ExportTypeBasedConfigManager exportTypeBasedConfigManager = mock(ExportTypeBasedConfigManager.class);
  private FolioExecutionContextHelper contextHelper = mock(FolioExecutionContextHelper.class);
  private AcqSchedulingProperties acqSchedulingProperties = mock(AcqSchedulingProperties.class);
  private EdifactOrdersExportJobScheduler exportJobScheduler = mock(EdifactOrdersExportJobScheduler.class);
  private TenantAttributes tenantAttributes = new TenantAttributes().moduleTo("3.0.0");

  private EdifactScheduledJobInitializer initializerWithQuartzDisabled;
  private EdifactScheduledJobInitializer initializerWithQuartzEnabled;

  @BeforeEach
  void before() {
    initializerWithQuartzDisabled = spy(new EdifactScheduledJobInitializer(exportTypeBasedConfigManager, contextHelper,
      acqSchedulingProperties, exportJobScheduler, false));
    initializerWithQuartzEnabled = spy(new EdifactScheduledJobInitializer(exportTypeBasedConfigManager, contextHelper,
      acqSchedulingProperties, exportJobScheduler, true));
  }

  @Test
  void shouldSkipScheduleAllJobsIfModuleIsNotRegisteredAndItIsMandatory() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();
    //When
    initializerWithQuartzDisabled.initAllScheduledJob(tenantAttributes);
    //Then
    verify(exportTypeBasedConfigManager, times(0)).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    verify(exportJobScheduler, times(0)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldSkipScheduleAllJobsIfModuleIsNotRegisteredAndItIsNotMandatory() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    doReturn(false).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();
    mockConfigCollection();
    //When
    initializerWithQuartzDisabled.initAllScheduledJob(tenantAttributes);
    //Then
    verify(exportTypeBasedConfigManager, times(1)).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    verify(exportJobScheduler, times(1)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldSkipScheduleJobsIfNoExportConfigs() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();
    doReturn(new ExportConfigCollection()).when(exportTypeBasedConfigManager).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    //When
    initializerWithQuartzDisabled.initAllScheduledJob(tenantAttributes);
    //Then
    verify(exportJobScheduler, times(0)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldScheduleJobsIfUpgradeToQuartzEnabledVersion() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    mockConfigCollection();

    initializerWithQuartzEnabled.initAllScheduledJob(tenantAttributes);

    verify(exportTypeBasedConfigManager, times(1)).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    verify(exportJobScheduler, times(1)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldSkipScheduleJobsIfUpgradeFromQuartzEnabledVersion() {
    doReturn(false).when(contextHelper).isModuleRegistered();

    initializerWithQuartzEnabled.initAllScheduledJob(new TenantAttributes().moduleFrom("3.0.0").moduleTo("4.0.0"));

    verify(exportTypeBasedConfigManager, times(0)).getConfigCollection(anyString(), eq(Integer.MAX_VALUE));
    verify(exportJobScheduler, times(0)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldScheduleJobsIfUpgradeWithForceSchedulesReload() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    mockConfigCollection();

    initializerWithQuartzEnabled.initAllScheduledJob(new TenantAttributes().moduleFrom("3.0.0").moduleTo("4.0.0")
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
