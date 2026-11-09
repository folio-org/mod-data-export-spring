package org.folio.des.config;

import org.folio.des.domain.dto.TransmissionMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Binds the param by API value ("File download"). Spring's default enum binding uses the constant name (FILE_DOWNLOAD).
 */
@Component
public class TransmissionMethodConverter implements Converter<String, TransmissionMethod> {

  @Override
  public TransmissionMethod convert(String source) {
    return TransmissionMethod.fromValue(source);
  }

}
