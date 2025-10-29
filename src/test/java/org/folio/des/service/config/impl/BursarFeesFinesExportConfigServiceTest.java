package org.folio.des.service.config.impl;

import static org.folio.des.support.TestUtils.getBursarExportConfig;
import static org.folio.des.support.TestUtils.setInternalState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.validator.BursarFeesFinesExportParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BursarFeesFinesExportConfigServiceTest {

  private ExportConfigRepository repository;
  private BursarExportScheduler bursarExportScheduler;
  private BursarFeesFinesExportConfigService service;

  @BeforeEach
  void setUp() {
    var validatorKey = "%s-%s".formatted(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class.getName());
    var exportConfigValidatorResolver = new ExportConfigValidatorResolver(Map.of(validatorKey, new BursarFeesFinesExportParametersValidator()));

    var defaultExportConfigMapper = new DefaultExportConfigMapper();
    var exportConfigMapperResolver = new ExportConfigMapperResolver(Map.of(), defaultExportConfigMapper);
    setInternalState(defaultExportConfigMapper, "objectMapper", new JacksonConfiguration().get());
    defaultExportConfigMapper.init();

    repository = Mockito.mock(ExportConfigRepository.class);
    bursarExportScheduler = Mockito.mock(BursarExportScheduler.class);
    service = new BursarFeesFinesExportConfigService(repository, defaultExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver, bursarExportScheduler);
  }

  @Test
  @DisplayName("Set new configuration")
  void testPostConfig() {
    var exportConfig = getBursarExportConfig();

    when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    var response = service.postConfig(exportConfig);

    assertEquals(exportConfig, response);
    verify(bursarExportScheduler, times(1)).scheduleBursarJob(exportConfig);
  }

  @Test
  @DisplayName("Update configuration")
  void testUpdateConfig() {
    var exportConfig = getBursarExportConfig();

    when(repository.findById(UUID.fromString(exportConfig.getId())))
      .thenReturn(java.util.Optional.of(new ExportConfigEntity()));

    service.updateConfig(exportConfig.getId(), exportConfig);

    verify(bursarExportScheduler, times(1)).scheduleBursarJob(exportConfig);
  }

}
