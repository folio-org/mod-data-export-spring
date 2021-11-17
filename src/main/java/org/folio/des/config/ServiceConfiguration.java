package org.folio.des.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.des.converter.BursarFeesFinesExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.validator.BurSarFeesFinesExportSpecificParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.validation.Validator;

@Configuration
public class ServiceConfiguration {

  @Bean
  BursarFeesFinesExportConfigConverter defaultConverter(ObjectMapper objectMapper) {
    return new BursarFeesFinesExportConfigConverter(objectMapper);
  }

  @Bean
  ExportConfigConverterResolver exportConfigConverterResolver(BursarFeesFinesExportConfigConverter defaultConverter) {
    Map<ExportType, Converter<ExportConfig, ModelConfiguration>> converters = new HashMap<>();
    converters.put(ExportType.BURSAR_FEES_FINES, defaultConverter);

    return new ExportConfigConverterResolver(converters, defaultConverter);
  }

  @Bean
  ExportConfigValidatorResolver exportConfigValidatorResolver() {
    Map<String, Validator> validators = new HashMap<>();
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class),
      new BurSarFeesFinesExportSpecificParametersValidator());

    return new ExportConfigValidatorResolver(validators);
  }

}
