package org.folio.des.config;

import java.util.HashMap;
import java.util.Map;

import org.folio.des.builder.job.AuthorityControlJobCommandBuilder;
import org.folio.des.builder.job.BulkEditQueryJobCommandBuilder;
import org.folio.des.builder.job.BurSarFeeFinesJobCommandBuilder;
import org.folio.des.builder.job.CirculationLogJobCommandBuilder;
import org.folio.des.builder.job.EHoldingsJobCommandBuilder;
import org.folio.des.builder.job.EdifactOrdersJobCommandBuilder;
import org.folio.des.builder.job.JobCommandBuilder;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.scheduling.EdifactSchedulingConfig;
import org.folio.des.converter.DefaultExportConfigToModelConfigConverter;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.converter.aqcuisition.EdifactExportConfigToModelConfigConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.scheduling.BursarExportScheduler;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.config.acquisition.EdifactOrdersExportService;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.service.config.impl.BurSarFeesFinesExportConfigService;
import org.folio.des.service.config.impl.ExportConfigServiceResolver;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.des.validator.BurSarFeesFinesExportParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
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
            ExportConfigConverterResolver  exportConfigConverterResolver,BursarExportScheduler bursarExportScheduler) {
    return new BurSarFeesFinesExportConfigService(client, defaultModelConfigToExportConfigConverter,
            exportConfigConverterResolver, exportConfigValidatorResolver,bursarExportScheduler);
  }

  @Bean
  EdifactOrdersExportService edifactOrdersExportService(ConfigurationClient client, ExportConfigValidatorResolver exportConfigValidatorResolver,
           DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
           ExportConfigConverterResolver  exportConfigConverterResolver,
           EdifactSchedulingConfig edifactSchedulingConfig) {
    return new EdifactOrdersExportService(client, defaultModelConfigToExportConfigConverter,
      exportConfigConverterResolver, exportConfigValidatorResolver, edifactSchedulingConfig.edifactOrdersExportJobScheduler());
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
                          EdifactOrdersExportService edifactOrdersExportService) {
    Map<ExportType, ExportConfigService> exportConfigServiceMap = new HashMap<>();
    exportConfigServiceMap.put(ExportType.BURSAR_FEES_FINES, burSarFeesFinesExportConfigService);
    exportConfigServiceMap.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactOrdersExportService);
    return new ExportConfigServiceResolver(exportConfigServiceMap);
  }

  @Bean JobCommandBuilderResolver jobCommandBuilderResolver(BulkEditQueryJobCommandBuilder bulkEditQueryJobCommandBuilder,
                          BurSarFeeFinesJobCommandBuilder burSarFeeFinesJobCommandBuilder,
                          CirculationLogJobCommandBuilder circulationLogJobCommandBuilder,
                          EdifactOrdersJobCommandBuilder edifactOrdersJobCommandBuilder,
                          EHoldingsJobCommandBuilder eHoldingsJobCommandBuilder,
                          AuthorityControlJobCommandBuilder authorityControlJobCommandBuilder) {
    Map<ExportType, JobCommandBuilder> converters = new HashMap<>();
    converters.put(ExportType.BULK_EDIT_QUERY, bulkEditQueryJobCommandBuilder);
    converters.put(ExportType.BURSAR_FEES_FINES, burSarFeeFinesJobCommandBuilder);
    converters.put(ExportType.CIRCULATION_LOG, circulationLogJobCommandBuilder);
    converters.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactOrdersJobCommandBuilder);
    converters.put(ExportType.E_HOLDINGS, eHoldingsJobCommandBuilder);
    converters.put(ExportType.AUTH_HEADINGS_UPDATES, authorityControlJobCommandBuilder);
    return new JobCommandBuilderResolver(converters);
  }

  @Bean
  EdifactScheduledJobInitializer edifactScheduledJobInitializer(ExportTypeBasedConfigManager exportTypeBasedConfigManager,
                    FolioExecutionContextHelper contextHelper,
                    EdifactSchedulingConfig edifactSchedulingConfig,
                    @Value("${folio.quartz.edifact.enabled}") boolean isQuartzEdifactEnabled) {
    return new EdifactScheduledJobInitializer(exportTypeBasedConfigManager, contextHelper,
      edifactSchedulingConfig.acqSchedulingProperties(), edifactSchedulingConfig.initEdifactOrdersExportJobScheduler(),
      isQuartzEdifactEnabled);
  }
}
