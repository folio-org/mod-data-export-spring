package org.folio.des.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultExportConfigToModelConfigConverter;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.converter.scheduling.DefaultExportConfigToTaskTriggersConverter;
import org.folio.des.converter.scheduling.TaskTriggerConverterResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.scheduling.ExportTaskTrigger;
import org.folio.des.scheduling.DefaultExportTriggerTaskRegistrar;
import org.folio.des.service.JobService;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.service.config.impl.BurSarFeesFinesExportConfigService;
import org.folio.des.service.config.impl.ExportConfigServiceResolver;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.des.validator.BurSarFeesFinesExportParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
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
  ExportConfigConverterResolver exportConfigConverterResolver(DefaultExportConfigToModelConfigConverter defaultExportConfigToModelConfigConverter) {
    Map<ExportType, Converter<ExportConfig, ModelConfiguration>> converters = new HashMap<>();
    converters.put(ExportType.BURSAR_FEES_FINES, defaultExportConfigToModelConfigConverter);

    return new ExportConfigConverterResolver(converters, defaultExportConfigToModelConfigConverter);
  }

  @Bean
  ExportConfigValidatorResolver exportConfigValidatorResolver(BurSarFeesFinesExportParametersValidator burSarFeesFinesExportParametersValidator) {
    Map<String, Validator> validators = new HashMap<>();
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class),
      burSarFeesFinesExportParametersValidator);

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
            DefaultExportConfigToModelConfigConverter defaultExportConfigToModelConfigConverter) {
    return new BurSarFeesFinesExportConfigService(client, defaultModelConfigToExportConfigConverter,
                    defaultExportConfigToModelConfigConverter, exportConfigValidatorResolver);
  }

  @Bean
  BaseExportConfigService baseExportConfigService(ConfigurationClient client, ExportConfigValidatorResolver exportConfigValidatorResolver,
    DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
    DefaultExportConfigToModelConfigConverter defaultExportConfigToModelConfigConverter) {
    return new BaseExportConfigService(client, defaultModelConfigToExportConfigConverter,
      defaultExportConfigToModelConfigConverter, exportConfigValidatorResolver);
  }

  @Bean
  ExportConfigServiceResolver exportConfigServiceResolver(BurSarFeesFinesExportConfigService burSarFeesFinesExportConfigService) {
    Map<ExportType, ExportConfigService> exportConfigServiceMap = new HashMap<>();
    exportConfigServiceMap.put(ExportType.BURSAR_FEES_FINES, burSarFeesFinesExportConfigService);
    return new ExportConfigServiceResolver(exportConfigServiceMap);
  }

  @Bean DefaultExportTriggerTaskRegistrar defaultExportTriggerTaskRegistrar(FolioExecutionContextHelper contextHelper,
            JobService jobService, TaskTriggerConverterResolver taskTriggerConverterResolver) {
    return new DefaultExportTriggerTaskRegistrar(contextHelper, new ThreadPoolTaskScheduler(), jobService, taskTriggerConverterResolver);
  }

  @Bean TaskTriggerConverterResolver taskTriggerConverterResolver(DefaultExportConfigToTaskTriggersConverter defaultExportConfigToTaskTriggersConverter) {
    Map<ExportType, Converter<ExportConfig, List<ExportTaskTrigger>>> converters = new HashMap<>();
    converters.put(ExportType.BATCH_VOUCHER_EXPORT, defaultExportConfigToTaskTriggersConverter);

    return new TaskTriggerConverterResolver(converters, defaultExportConfigToTaskTriggersConverter);
  }
}
