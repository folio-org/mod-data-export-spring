package org.folio.des.config;

import java.util.HashMap;
import java.util.Map;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultExportConfigToModelConfigConverter;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.config.impl.ExportConfigServiceImpl;
import org.folio.des.service.config.impl.ExportConfigServiceResolver;
import org.folio.des.validator.BurSarFeesFinesExportParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ServiceConfiguration {
  @Bean DefaultExportConfigToModelConfigConverter defaultConverter(ObjectMapper objectMapper) {
    return new DefaultExportConfigToModelConfigConverter(objectMapper);
  }

  @Bean DefaultModelConfigToExportConfigConverter defaultModelConfigurationToExportConfigConverter(ObjectMapper objectMapper) {
    return new DefaultModelConfigToExportConfigConverter(objectMapper);
  }

  @Bean
  ExportConfigConverterResolver exportConfigConverterResolver(DefaultExportConfigToModelConfigConverter defaultConverter) {
    Map<ExportType, Converter<ExportConfig, ModelConfiguration>> converters = new HashMap<>();
    converters.put(ExportType.BURSAR_FEES_FINES, defaultConverter);

    return new ExportConfigConverterResolver(converters, defaultConverter);
  }

  @Bean
  BurSarFeesFinesExportParametersValidator burSarFeesFinesExportParametersValidator() {
    return new BurSarFeesFinesExportParametersValidator();
  }

  @Bean
  ExportConfigValidatorResolver exportConfigValidatorResolver(BurSarFeesFinesExportParametersValidator burSarFeesFinesExportParametersValidator) {
    Map<String, Validator> validators = new HashMap<>();
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class),
      burSarFeesFinesExportParametersValidator);

    return new ExportConfigValidatorResolver(validators);
  }

//  @Bean
//  ExportTypeBasedConfigManager exportTypeBasedConfigManager(ExportConfigServiceResolver exportConfigServiceResolver,
//                    DefaultModelConfigurationToExportConfigConverter defaultModelConfigurationToExportConfigConverter,
//                    ExportConfigService defaultExportConfigService, ConfigurationClient client) {
//    return new ExportTypeBasedConfigManager(client, exportConfigServiceResolver,
//                    defaultExportConfigService, defaultModelConfigurationToExportConfigConverter);
//  }
//
  @Bean
  ExportConfigServiceImpl exportConfigService(ConfigurationClient configurationClient, ExportConfigValidatorResolver exportConfigValidatorResolver,
            DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter, ObjectMapper objectMapper) {
    return new ExportConfigServiceImpl(configurationClient, defaultModelConfigToExportConfigConverter, exportConfigValidatorResolver, objectMapper);
  }

  @Bean
  ExportConfigServiceResolver exportConfigServiceResolver(ExportConfigServiceImpl exportConfigService) {
    Map<ExportType, ExportConfigService> exportConfigServiceMap = new HashMap<>();
    exportConfigServiceMap.put(ExportType.BURSAR_FEES_FINES, exportConfigService);
    return new ExportConfigServiceResolver(exportConfigServiceMap);
  }

}
