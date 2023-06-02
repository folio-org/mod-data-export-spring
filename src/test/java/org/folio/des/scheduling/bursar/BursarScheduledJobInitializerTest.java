package org.folio.des.scheduling.bursar;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.BursarExportScheduler;
import org.folio.des.service.config.ExportConfigService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class BursarScheduledJobInitializerTest {
  private ExportConfigService burSarExportConfigService = mock(ExportConfigService.class);
  private BursarExportScheduler bursarExportScheduler = mock(BursarExportScheduler.class);

  private BursarScheduledJobInitializer bursarScheduledJobInitializer;

  @BeforeEach
  void before() {
    bursarScheduledJobInitializer = spy(new BursarScheduledJobInitializer(burSarExportConfigService, bursarExportScheduler));
  }

  @Test
  void shouldInitiateBursarJob() {
    when(burSarExportConfigService.getFirstConfig())
      .thenReturn(Optional.of(new ExportConfig()));
    bursarScheduledJobInitializer.initAllScheduledJob(new TenantAttributes().moduleTo("3.0.0"));
    verify(bursarExportScheduler, times(1)).scheduleBursarJob(any());
  }

  @Test
  void shouldNotInitiateBursarJob() {
    when(burSarExportConfigService.getFirstConfig())
      .thenReturn(Optional.empty());
    bursarScheduledJobInitializer.initAllScheduledJob(new TenantAttributes().moduleTo("3.0.0"));
    verify(bursarExportScheduler, times(0)).scheduleBursarJob(any());
  }
}
