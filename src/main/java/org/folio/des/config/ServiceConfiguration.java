package org.folio.des.config;

import java.util.HashMap;
import java.util.Map;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultExportConfigToModelConfigConverter;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.converter.aqcuisition.EdifactExportConfigToModelConfigConverter;
import org.folio.des.converter.scheduling.DefaultExportConfigToTaskTriggersConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.scheduling.DefaultExportTriggerTaskRegistrar;
import org.folio.des.service.JobService;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.service.config.impl.BurSarFeesFinesExportConfigService;
import org.folio.des.service.config.impl.ExportConfigServiceResolver;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.des.validator.BurSarFeesFinesExportParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.validation.Validator;

@Configuration
@ComponentScan("org.folio.des")
public class ServiceConfiguration {
  @Bean
  ExportConfigConverterResolver exportConfigConverterResolver(DefaultExportConfigToModelConfigConverter defaultExportConfigToModelConfigConverter,
                      EdifactExportConfigToModelConfigConverter edifactExportConfigToModelConfigConverter) {
    Map<ExportType, Converter<ExportConfig, ModelConfiguration>> converters = new HashMap<>();
    converters.put(ExportType.BURSAR_FEES_FINES, defaultExportConfigToModelConfigConverter);
    converters.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactExportConfigToModelConfigConverter);
    return new ExportConfigConverterResolver(converters, defaultExportConfigToModelConfigConverter);
  }

  @Bean
  ExportConfigValidatorResolver exportConfigValidatorResolver(BurSarFeesFinesExportParametersValidator burSarFeesFinesExportParametersValidator,
                      EdifactOrdersExportParametersValidator edifactOrdersExportParametersValidator) {
    Map<String, Validator> validators = new HashMap<>();
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class),
      burSarFeesFinesExportParametersValidator);
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.EDIFACT_ORDERS_EXPORT, ExportTypeSpecificParameters.class),
      edifactOrdersExportParametersValidator);
    return new ExportConfigValidatorResolver(validators);
  }

  @Bean
  ExportTypeBasedConfigManager exportTypeBasedConfigManager(ConfigurationClient client,
                      ExportConfigServiceResolver exportConfigServiceResolver,
                      BaseExportConfigService baseExportConfigService,
                      DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter) {
    return new ExportTypeBasedConfigManager(client, exportConfigServiceResolver,
                      baseExportConfigService, defaultModelConfigToExportConfigConverter);
  }

  @Bean
  BurSarFeesFinesExportConfigService burSarExportConfigService(ConfigurationClient client, ExportConfigValidatorResolver exportConfigValidatorResolver,
            DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
            ExportConfigConverterResolver  exportConfigConverterResolver) {
    return new BurSarFeesFinesExportConfigService(client, defaultModelConfigToExportConfigConverter,
            exportConfigConverterResolver, exportConfigValidatorResolver);
  }

  @Bean
  BaseExportConfigService baseExportConfigService(ConfigurationClient client, ExportConfigValidatorResolver exportConfigValidatorResolver,
                        DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
                        ExportConfigConverterResolver exportConfigConverterResolver) {
    return new BaseExportConfigService(client, defaultModelConfigToExportConfigConverter,
                        exportConfigConverterResolver, exportConfigValidatorResolver);
  }


  @Bean
  ExportConfigServiceResolver exportConfigServiceResolver(BurSarFeesFinesExportConfigService burSarFeesFinesExportConfigService,
                                  BaseExportConfigService baseExportConfigService) {
    Map<ExportType, ExportConfigService> exportConfigServiceMap = new HashMap<>();
    exportConfigServiceMap.put(ExportType.BURSAR_FEES_FINES, burSarFeesFinesExportConfigService);
    exportConfigServiceMap.put(ExportType.EDIFACT_ORDERS_EXPORT, baseExportConfigService);
    return new ExportConfigServiceResolver(exportConfigServiceMap);
  }

  @Bean DefaultExportTriggerTaskRegistrar defaultExportTriggerTaskRegistrar(FolioExecutionContextHelper contextHelper,
            JobService jobService, DefaultExportConfigToTaskTriggersConverter triggerConverter) {
    return new DefaultExportTriggerTaskRegistrar(contextHelper, new ThreadPoolTaskScheduler(), jobService, triggerConverter);
  }
}
