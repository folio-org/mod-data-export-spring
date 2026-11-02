package org.folio.des.config;

import org.folio.des.domain.dto.ConfigTransmissionMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Binds the param by API value ("File download"). Spring's default enum binding uses the constant name (FILE_DOWNLOAD).
 */
@Component
public class ConfigTransmissionMethodConverter implements Converter<String, ConfigTransmissionMethod> {

  @Override
  public ConfigTransmissionMethod convert(String source) {
    return ConfigTransmissionMethod.fromValue(source);
  }

}
