package org.folio.des.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.folio.des.builder.job.AuthorityControlJobCommandBuilder;
import org.folio.des.builder.job.BursarFeeFinesJobCommandBuilder;
import org.folio.des.builder.job.CirculationLogJobCommandBuilder;
import org.folio.des.builder.job.EHoldingsJobCommandBuilder;
import org.folio.des.builder.job.EdifactOrdersJobCommandBuilder;
import org.folio.des.builder.job.JobCommandBuilder;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.mapper.BaseExportConfigMapper;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.mapper.acquisition.ClaimsExportConfigMapper;
import org.folio.des.mapper.acquisition.EdifactExportConfigMapper;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.scheduling.acquisition.EdifactScheduledJobInitializer;
import org.folio.des.scheduling.quartz.QuartzConstants;
import org.folio.des.scheduling.quartz.QuartzExportJobScheduler;
import org.folio.des.scheduling.quartz.ScheduledJobsRemover;
import org.folio.des.scheduling.quartz.converter.acquisition.ExportConfigToEdifactJobDetailConverter;
import org.folio.des.scheduling.quartz.converter.acquisition.ExportConfigToEdifactTriggerConverter;
import org.folio.des.scheduling.quartz.job.acquisition.EdifactJobKeyResolver;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.service.config.acquisition.ClaimsExportService;
import org.folio.des.service.config.acquisition.EdifactOrdersExportService;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.service.config.impl.BursarFeesFinesExportConfigService;
import org.folio.des.service.config.impl.ExportConfigServiceResolver;
import org.folio.des.service.config.impl.ExportTypeBasedConfigManager;
import org.folio.des.validator.BursarFeesFinesExportParametersValidator;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.des.validator.acquisition.ClaimsExportParametersValidator;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;

@Configuration
@ComponentScan("org.folio.des")
public class ServiceConfiguration {

  @Bean
  ExportConfigMapperResolver exportConfigConverterResolver(DefaultExportConfigMapper defaultExportConfigMapper,
                                                           EdifactExportConfigMapper edifactExportConfigMapper,
                                                           ClaimsExportConfigMapper claimsExportConfigMapper) {
    var converters = new HashMap<ExportType, BaseExportConfigMapper>();
    converters.put(ExportType.BURSAR_FEES_FINES, defaultExportConfigMapper);
    converters.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactExportConfigMapper);
    converters.put(ExportType.CLAIMS, claimsExportConfigMapper);
    return new ExportConfigMapperResolver(converters, defaultExportConfigMapper);
  }

  @Bean
  ExportConfigValidatorResolver exportConfigValidatorResolver(BursarFeesFinesExportParametersValidator bursarFeesFinesExportParametersValidator,
                                                              EdifactOrdersExportParametersValidator edifactOrdersExportParametersValidator,
                                                              ClaimsExportParametersValidator claimsExportParametersValidator) {
    Map<String, Validator> validators = new HashMap<>();
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class), bursarFeesFinesExportParametersValidator);
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.EDIFACT_ORDERS_EXPORT, ExportTypeSpecificParameters.class), edifactOrdersExportParametersValidator);
    validators.put(ExportConfigValidatorResolver.buildKey(ExportType.CLAIMS, ExportTypeSpecificParameters.class), claimsExportParametersValidator);
    return new ExportConfigValidatorResolver(validators);
  }

  @Bean
  ExportTypeBasedConfigManager exportTypeBasedConfigManager(ExportConfigServiceResolver exportConfigServiceResolver,
                                                            BaseExportConfigService defaultExportConfigService) {
    return new ExportTypeBasedConfigManager(exportConfigServiceResolver, defaultExportConfigService);
  }

  @Bean
  BursarFeesFinesExportConfigService bursarExportConfigService(ExportConfigRepository repository,
                                                               DefaultExportConfigMapper defaultExportConfigMapper,
                                                               ExportConfigMapperResolver exportConfigMapperResolver,
                                                               ExportConfigValidatorResolver exportConfigValidatorResolver,
                                                               BursarExportScheduler bursarExportScheduler) {
    return new BursarFeesFinesExportConfigService(repository, defaultExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver, bursarExportScheduler);
  }

  @Bean
  EdifactOrdersExportService edifactOrdersExportService(ExportConfigRepository repository,
                                                        EdifactExportConfigMapper edifactExportConfigMapper,
                                                        ExportConfigMapperResolver exportConfigMapperResolver,
                                                        ExportConfigValidatorResolver exportConfigValidatorResolver,
                                                        ExportJobScheduler exportJobScheduler) {
    return new EdifactOrdersExportService(repository, edifactExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver, exportJobScheduler);
  }

  @Bean
  ClaimsExportService claimsExportService(ExportConfigRepository repository,
                                          ClaimsExportConfigMapper claimsExportConfigMapper,
                                          ExportConfigMapperResolver exportConfigMapperResolver,
                                          ExportConfigValidatorResolver exportConfigValidatorResolver) {
    return new ClaimsExportService(repository, claimsExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver);
  }

  @Bean
  BaseExportConfigService defaultExportConfigService(ExportConfigRepository repository,
                                                     DefaultExportConfigMapper defaultExportConfigMapper,
                                                     ExportConfigMapperResolver exportConfigMapperResolver,
                                                     ExportConfigValidatorResolver exportConfigValidatorResolver) {
    return new BaseExportConfigService(repository, defaultExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver);
  }

  @Bean
  ExportConfigServiceResolver exportConfigServiceResolver(BursarFeesFinesExportConfigService bursarFeesFinesExportConfigService,
                                                          EdifactOrdersExportService edifactOrdersExportService,
                                                          ClaimsExportService claimsExportService) {
    var exportConfigServiceMap = new HashMap<ExportType, ExportConfigService>();
    exportConfigServiceMap.put(ExportType.BURSAR_FEES_FINES, bursarFeesFinesExportConfigService);
    exportConfigServiceMap.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactOrdersExportService);
    exportConfigServiceMap.put(ExportType.CLAIMS, claimsExportService);
    return new ExportConfigServiceResolver(exportConfigServiceMap);
  }

  @Bean
  JobCommandBuilderResolver jobCommandBuilderResolver(BursarFeeFinesJobCommandBuilder bursarFeeFinesJobCommandBuilder,
                                                      CirculationLogJobCommandBuilder circulationLogJobCommandBuilder,
                                                      EdifactOrdersJobCommandBuilder edifactOrdersJobCommandBuilder,
                                                      EHoldingsJobCommandBuilder eHoldingsJobCommandBuilder,
                                                      AuthorityControlJobCommandBuilder authorityControlJobCommandBuilder) {
    Map<ExportType, JobCommandBuilder> converters = new HashMap<>();
    converters.put(ExportType.BURSAR_FEES_FINES, bursarFeeFinesJobCommandBuilder);
    converters.put(ExportType.CIRCULATION_LOG, circulationLogJobCommandBuilder);
    converters.put(ExportType.EDIFACT_ORDERS_EXPORT, edifactOrdersJobCommandBuilder);
    converters.put(ExportType.CLAIMS, edifactOrdersJobCommandBuilder);
    converters.put(ExportType.E_HOLDINGS, eHoldingsJobCommandBuilder);
    converters.put(ExportType.AUTH_HEADINGS_UPDATES, authorityControlJobCommandBuilder);
    return new JobCommandBuilderResolver(converters);
  }

  @Bean
  EdifactScheduledJobInitializer edifactScheduledJobInitializer(ExportTypeBasedConfigManager exportTypeBasedConfigManager,
                                                                ExportJobScheduler exportJobScheduler) {
    return new EdifactScheduledJobInitializer(exportTypeBasedConfigManager, exportJobScheduler);
  }

  @Bean
  public ExportJobScheduler edifactOrdersExportJobScheduler(Scheduler scheduler,
                                                            ExportConfigToEdifactTriggerConverter edifactTriggerConverter,
                                                            ExportConfigToEdifactJobDetailConverter edifactJobDetailConverter,
                                                            EdifactJobKeyResolver edifactJobKeyResolver) {
    return new QuartzExportJobScheduler(scheduler, edifactTriggerConverter, edifactJobDetailConverter, edifactJobKeyResolver);
  }

  @Bean
  ScheduledJobsRemover scheduledJobsRemover(Scheduler scheduler) {
    return new ScheduledJobsRemover(scheduler, List.of(QuartzConstants.EDIFACT_ORDERS_EXPORT_GROUP_NAME, QuartzConstants.BURSAR_EXPORT_GROUP_NAME));
  }
}

