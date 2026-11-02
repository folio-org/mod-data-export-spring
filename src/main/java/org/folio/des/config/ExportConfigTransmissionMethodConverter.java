package org.folio.des.config;

import org.folio.des.domain.dto.ExportConfigTransmissionMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Binds the param by API value ("File download"). Spring's default enum binding uses the constant name (FILE_DOWNLOAD).
 */
@Component
public class ExportConfigTransmissionMethodConverter implements Converter<String, ExportConfigTransmissionMethod> {

  @Override
  public ExportConfigTransmissionMethod convert(String source) {
    return ExportConfigTransmissionMethod.fromValue(source);
  }

}
