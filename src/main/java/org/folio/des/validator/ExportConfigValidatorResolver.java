package org.folio.des.validator;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ExportType;
import org.springframework.validation.Validator;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;


@AllArgsConstructor
@Log4j2
public class ExportConfigValidatorResolver {
  public static final String HYPHEN_SEPARATOR = "-";

  private final Map<String, Validator> validators;

  public Optional<Validator> resolve(ExportType exportType, Class<?> targetClass) {
    String key = buildKey(exportType, targetClass);
    return Optional.ofNullable(validators.get(key));
  }

  public static String buildKey(ExportType exportType, Class<?> targetClass) {
    return Optional.ofNullable(exportType).map(ExportType::getValue).orElse(StringUtils.EMPTY) + HYPHEN_SEPARATOR + targetClass.getName();
  }
}
