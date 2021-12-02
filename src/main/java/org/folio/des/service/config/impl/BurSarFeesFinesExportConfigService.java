package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;

import java.util.Optional;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultExportConfigToModelConfigConverter;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.validator.ExportConfigValidatorResolver;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BurSarFeesFinesExportConfigService extends BaseExportConfigService {

  public BurSarFeesFinesExportConfigService(ConfigurationClient client,
                                  DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
                                  DefaultExportConfigToModelConfigConverter defaultExportConfigToModelConfigConverter,
                                  ExportConfigValidatorResolver exportConfigValidatorResolver) {
    super(client, defaultModelConfigToExportConfigConverter, defaultExportConfigToModelConfigConverter,
      exportConfigValidatorResolver);
  }

  @Override
  public ExportConfigCollection getConfigCollection(String query) {
    return getFirstConfig().map(this::createExportConfigCollection).orElse(emptyExportConfigCollection());
  }

  @Override
  public Optional<ExportConfig> getFirstConfig() {
    var configurationCollection = client.getConfigurations(String.format(DEFAULT_CONFIG_QUERY, DEFAULT_CONFIG_NAME));
    if (configurationCollection.getTotalRecords() == 0) {
      return Optional.empty();
    }
    var config = defaultModelConfigToExportConfigConverter.convert(configurationCollection.getConfigs().get(0));
    return Optional.of(config);
  }

  private ExportConfigCollection createExportConfigCollection(ExportConfig exportConfig) {
    var configCollection = new ExportConfigCollection();
    configCollection.addConfigsItem(exportConfig);
    configCollection.setTotalRecords(1);
    return configCollection;
  }

  private ExportConfigCollection emptyExportConfigCollection() {
    var configCollection = new ExportConfigCollection();
    configCollection.setTotalRecords(0);
    return configCollection;
  }
}
