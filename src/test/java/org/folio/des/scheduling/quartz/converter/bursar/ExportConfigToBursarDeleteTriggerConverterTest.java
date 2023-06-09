package org.folio.des.scheduling.quartz.converter.bursar;

import static org.folio.des.support.BaseTest.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverter;
import org.folio.des.scheduling.quartz.converter.ScheduleParametersToTriggerConverterImpl;
import org.folio.des.scheduling.quartz.trigger.ExportTrigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ExportConfigToBursarDeleteTriggerConverterTest {

  private static final String EXPORT_CONFIG_ID = UUID.randomUUID().toString();
  private static final String TRIGGER_GROUP = TENANT + "_" + QuartzConstants.BURSAR_EXPORT_DELETE_GROUP_NAME;
  private final ScheduleParametersToTriggerConverter scheduleParamsToTriggerConverter = new ScheduleParametersToTriggerConverterImpl();
  private final ExportConfigToBursarDeleteTriggerConverter converter = new ExportConfigToBursarDeleteTriggerConverter(scheduleParamsToTriggerConverter, "UTC");

  @Test
  void shouldCreateEnabledTrigger() {
    ExportConfig config = createConfig();
    ExportTrigger exportTrigger = converter.convert(config);
    assertNotNull(exportTrigger);
    assertFalse(exportTrigger.isDisabled());
    var triggers = exportTrigger.triggers();
    assertNotNull(triggers);
    assertEquals(1, triggers.size());
    assertEquals(TRIGGER_GROUP, triggers.iterator().next().getKey().getGroup());
  }
  private ExportConfig createConfig() {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(EXPORT_CONFIG_ID);
    exportConfig.setType(ExportType.BURSAR_FEES_FINES);
    exportConfig.setTenant(TENANT);
    return exportConfig;
  }
}
