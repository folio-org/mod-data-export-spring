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

class ExportConfigToBursarTriggerConverterTest {

  private static final String EXPORT_CONFIG_ID = UUID.randomUUID().toString();
  public static final String SCHEDULE_TIME = "20:59:00.000Z";

  private static final String TRIGGER_GROUP = TENANT + "_" + QuartzConstants.BURSAR_EXPORT_GROUP_NAME;

  private final ScheduleParametersToTriggerConverter scheduleParamsToTriggerConverter = new ScheduleParametersToTriggerConverterImpl();
  private final ExportConfigToBursarTriggerConverter converter = new ExportConfigToBursarTriggerConverter(scheduleParamsToTriggerConverter, "UTC");

  @ParameterizedTest
  @EnumSource(value = ExportConfig.SchedulePeriodEnum.class, names = {"HOUR", "DAY", "WEEK"})
  void shouldCreateEnabledTrigger(ExportConfig.SchedulePeriodEnum schedulePeriodEnum) {
    ExportConfig config = createConfig(schedulePeriodEnum);
    ExportTrigger exportTrigger = converter.convert(config);
    assertNotNull(exportTrigger);
    assertFalse(exportTrigger.isDisabled());
    var triggers = exportTrigger.triggers();
    assertNotNull(triggers);
    assertEquals(1, triggers.size());
    assertEquals(TRIGGER_GROUP, triggers.iterator().next().getKey().getGroup());
  }

  @Test
  void shouldCreateDisabledExportTriggerIfScheduledParameterTypeIsNone() {
    ExportConfig config = createConfig(ExportConfig.SchedulePeriodEnum.NONE);
    ExportTrigger exportTrigger = converter.convert(config);
    assertNotNull(exportTrigger);
    assertTrue(exportTrigger.isDisabled());
    assertTrue(CollectionUtils.isEmpty(exportTrigger.triggers()));
  }

  private ExportConfig createConfig(ExportConfig.SchedulePeriodEnum schedulePeriodEnum) {
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(EXPORT_CONFIG_ID);
    exportConfig.setType(ExportType.BURSAR_FEES_FINES);
    exportConfig.setTenant(TENANT);
    exportConfig.setScheduleTime(SCHEDULE_TIME);
    exportConfig.setSchedulePeriod(schedulePeriodEnum);
    exportConfig.setScheduleFrequency(1);
    if (ExportConfig.SchedulePeriodEnum.WEEK.equals(schedulePeriodEnum)) {
      exportConfig.setWeekDays(List.of(ExportConfig.WeekDaysEnum.TUESDAY));
    }
    return exportConfig;
  }
}
