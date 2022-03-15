package org.folio.des.scheduling.acquisition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EdifactScheduledJobInitializerTest {
  private ExportTypeBasedConfigManager exportTypeBasedConfigManager = mock(ExportTypeBasedConfigManager.class);
  private FolioExecutionContextHelper contextHelper = mock(FolioExecutionContextHelper.class);
  private AcqSchedulingProperties acqSchedulingProperties = mock(AcqSchedulingProperties.class);
  private EdifactOrdersExportJobScheduler exportJobScheduler = mock(EdifactOrdersExportJobScheduler.class);

  private EdifactScheduledJobInitializer initializer;

  @BeforeEach
  void before() {
    initializer = spy(new EdifactScheduledJobInitializer(exportTypeBasedConfigManager, contextHelper, acqSchedulingProperties, exportJobScheduler));
  }

  @Test
  void shouldSkipScheduleAllJobsIfModuleIsNotRegisteredAndItIsMandatory() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();
    //When
    initializer.initAllScheduledJob();
    //Then
    verify(exportTypeBasedConfigManager, times(0)).getConfigCollection(anyString());
    verify(exportJobScheduler, times(0)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldSkipScheduleAllJobsIfModuleIsNotRegisteredAndItIsNotMandatory() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    doReturn(false).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();
    ExportConfigCollection configCollection = new ExportConfigCollection();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(UUID.randomUUID().toString());
    configCollection.addConfigsItem(exportConfig);
    doReturn(configCollection).when(exportTypeBasedConfigManager).getConfigCollection(anyString());
    //When
    initializer.initAllScheduledJob();
    //Then
    verify(exportTypeBasedConfigManager, times(1)).getConfigCollection(anyString());
    verify(exportJobScheduler, times(1)).scheduleExportJob(any(ExportConfig.class));
  }

  @Test
  void shouldSkipScheduleJobsIfNoExportConfigs() {
    doReturn(false).when(contextHelper).isModuleRegistered();
    doReturn(true).when(acqSchedulingProperties).isRunOnlyIfModuleRegistered();
    doReturn(new ExportConfigCollection()).when(exportTypeBasedConfigManager).getConfigCollection(anyString());
    //When
    initializer.initAllScheduledJob();
    //Then
    verify(exportJobScheduler, times(0)).scheduleExportJob(any(ExportConfig.class));
  }
}
