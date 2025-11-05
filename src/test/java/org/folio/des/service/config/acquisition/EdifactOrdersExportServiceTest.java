package org.folio.des.service.config.acquisition;

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
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.mapper.acquisition.EdifactExportConfigMapperImpl;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.folio.des.validator.acquisition.EdifactOrdersScheduledParamsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdifactOrdersExportServiceTest {

  private static final UUID CONFIG_ID = UUID.randomUUID();
  private static final UUID VENDOR_ID = UUID.fromString("046b6c7f-0b8a-43b9-b35d-6489e6daee91");
  private static final ExportConfig EDIFACT_EXPORT_CONFIG = new ExportConfig()
    .id(CONFIG_ID.toString())
    .type(ExportType.EDIFACT_ORDERS_EXPORT)
    .configName("%s_%s_%s".formatted(ExportType.EDIFACT_ORDERS_EXPORT.getValue(), VENDOR_ID, CONFIG_ID))
    .exportTypeSpecificParameters(new ExportTypeSpecificParameters()
      .vendorEdiOrdersExportConfig(new VendorEdiOrdersExportConfig()
        .configName("edi_test_config")
        .vendorId(VENDOR_ID)
        .ediSchedule(new EdiSchedule()
          .enableScheduledExport(true)
          .scheduleParameters(new ScheduleParameters()
            .schedulePeriod(ScheduleParameters.SchedulePeriodEnum.HOUR)
            .scheduleFrequency(1)
            .scheduleTime("15:00:00")))));

  private ExportConfigRepository repository;
  private ExportJobScheduler edifactOrdersExportJobScheduler;
  private EdifactOrdersExportService service;

  @BeforeEach
  void setUp() {
    var validatorKey = "%s-%s".formatted(ExportType.EDIFACT_ORDERS_EXPORT, ExportTypeSpecificParameters.class.getName());
    var validator = new EdifactOrdersExportParametersValidator(new EdifactOrdersScheduledParamsValidator());
    var exportConfigValidatorResolver = new ExportConfigValidatorResolver(Map.of(validatorKey, validator));

    var defaultExportConfigMapper = new DefaultExportConfigMapper();
    var edifactExportConfigMapper = new EdifactExportConfigMapperImpl();
    var exportConfigMapperResolver = new ExportConfigMapperResolver(Map.of(ExportType.EDIFACT_ORDERS_EXPORT, edifactExportConfigMapper), defaultExportConfigMapper);
    setInternalState(edifactExportConfigMapper, "objectMapper", new JacksonConfiguration().entityObjectMapper());
    setInternalState(edifactExportConfigMapper, "validator", validator);

    repository = Mockito.mock(ExportConfigRepository.class);
    edifactOrdersExportJobScheduler = Mockito.mock(ExportJobScheduler.class);
    service = new EdifactOrdersExportService(repository, edifactExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver, edifactOrdersExportJobScheduler);
  }

  @Test
  @DisplayName("Set new configuration")
  void testPostConfig() {
    when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    var response = service.postConfig(EDIFACT_EXPORT_CONFIG);

    assertEquals(EDIFACT_EXPORT_CONFIG, response);
    verify(edifactOrdersExportJobScheduler, times(1)).scheduleExportJob(EDIFACT_EXPORT_CONFIG);
  }

  @Test
  @DisplayName("Update configuration")
  void testUpdateConfig() {
    when(repository.findById(UUID.fromString(EDIFACT_EXPORT_CONFIG.getId())))
      .thenReturn(java.util.Optional.of(new ExportConfigEntity()));

    service.updateConfig(EDIFACT_EXPORT_CONFIG.getId(), EDIFACT_EXPORT_CONFIG);

    verify(edifactOrdersExportJobScheduler, times(1)).scheduleExportJob(EDIFACT_EXPORT_CONFIG);
  }

}
