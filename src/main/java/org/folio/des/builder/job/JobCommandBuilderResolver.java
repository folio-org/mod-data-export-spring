package org.folio.des.builder.job;

import java.util.Map;
import java.util.Optional;

import org.folio.des.domain.dto.ExportType;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class JobCommandBuilderResolver {
  private final Map<ExportType, JobCommandBuilder> converters;

  public Optional<JobCommandBuilder> resolve(ExportType type) {
    return Optional.ofNullable(converters.get(type));
  }
}
