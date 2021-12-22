package org.folio.des.converter;

import java.util.Collections;
import java.util.List;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.ExportTrigger;
import org.folio.des.scheduling.base.BaseExportTaskTrigger;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.springframework.core.convert.converter.Converter;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.support.SimpleTriggerContext;

@AllArgsConstructor
@Log4j2
public class DefaultExportConfigToTaskTriggersConverter implements Converter<ExportConfig, List<ExportTaskTrigger>>  {
  @Override
  public List<ExportTaskTrigger> convert(ExportConfig exportConfig) {
    if (ExportConfig.SchedulePeriodEnum.NONE != exportConfig.getSchedulePeriod()) {
      ExportTrigger exportTrigger = new ExportTrigger();
      exportTrigger.setConfig(exportConfig);
      BaseExportTaskTrigger baseExportTaskTrigger = new BaseExportTaskTrigger(exportTrigger);
      return List.of(baseExportTaskTrigger);
    }
    return Collections.emptyList();
  }
}
