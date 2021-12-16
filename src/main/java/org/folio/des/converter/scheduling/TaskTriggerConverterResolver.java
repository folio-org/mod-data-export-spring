package org.folio.des.converter.scheduling;

import java.util.List;
import java.util.Map;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.scheduling.ExportTaskTrigger;
import org.springframework.core.convert.converter.Converter;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
public class TaskTriggerConverterResolver {
  private final Map<ExportType, Converter<ExportConfig, List<ExportTaskTrigger>>> converters;
  private final Converter<ExportConfig, List<ExportTaskTrigger>> defaultConverter;

  public Converter<ExportConfig, List<ExportTaskTrigger>> resolve(ExportType exportType) {
    return converters.getOrDefault(exportType, defaultConverter);
  }
}
