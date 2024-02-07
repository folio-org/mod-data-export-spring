package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;

import java.util.Optional;
import java.util.function.Function;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportConfigWithLegacyBursar;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.validator.ExportConfigValidatorResolver;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BursarFeesFinesExportConfigService extends BaseExportConfigService {

  private final BursarExportScheduler bursarExportScheduler;

  public BursarFeesFinesExportConfigService(ConfigurationClient client,
                                            DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
                                            ExportConfigConverterResolver exportConfigConverterResolver,
                                            ExportConfigValidatorResolver exportConfigValidatorResolver, BursarExportScheduler bursarExportScheduler) {
    super(client, defaultModelConfigToExportConfigConverter, exportConfigConverterResolver, exportConfigValidatorResolver);
    this.bursarExportScheduler = bursarExportScheduler;
  }

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    log.info("updateConfig:: starting Bursar updateConfig with configId:{}", configId);
    super.updateConfig(configId, exportConfig);
    bursarExportScheduler.scheduleBursarJob(exportConfig);
  }

  @Override
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    log.info("postConfig:: starting Bursar postConfig with configId:{}", exportConfig.getId());
    ModelConfiguration modelConfiguration = super.postConfig(exportConfig);
    bursarExportScheduler.scheduleBursarJob(exportConfig);
    return modelConfiguration;
  }


  @Override
  public ExportConfigCollection getConfigCollection(String query, Integer limit) {
    log.info("getConfigCollection:: the result doesn't base on a query.");
    return getFirstConfig().map(this::createExportConfigCollection).orElse(emptyExportConfigCollection());
  }

  @Override
  public Optional<ExportConfig> getFirstConfig() {
    return getFirstConfigGeneric(defaultModelConfigToExportConfigConverter::convert);
  }

  public Optional<ExportConfigWithLegacyBursar> getFirstConfigLegacy() {
    return getFirstConfigGeneric(defaultModelConfigToExportConfigConverter::convertLegacy);
  }

  public <T> Optional<T> getFirstConfigGeneric(Function<ModelConfiguration, T> converter) {
    ConfigurationCollection configurationCollection = client.getConfigurations(String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME), 1);
    if (configurationCollection.getTotalRecords() == 0) {
      return Optional.empty();
    }
    T config = converter.apply(configurationCollection.getConfigs().get(0));
    return Optional.of(config);
  }

  private ExportConfigCollection createExportConfigCollection(ExportConfig exportConfig) {
    var configCollection = new ExportConfigCollection();
    configCollection.addConfigsItem(exportConfig);
    configCollection.setTotalRecords(1);
    log.info("createExportConfigCollection:: configCollection={}.", configCollection);
    return configCollection;
  }

  private ExportConfigCollection emptyExportConfigCollection() {
    log.debug("emptyExportConfigCollection:: ");
    var configCollection = new ExportConfigCollection();
    configCollection.setTotalRecords(0);
    return configCollection;
  }
}
