package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;
import static org.folio.des.service.config.ExportConfigConstants.MODULE_NAME;

import java.util.Optional;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;


@RequiredArgsConstructor
@Log4j2
public class ExportConfigServiceImpl implements ExportConfigService {
  private static final String CONFIG_DESCRIPTION = "Data export configuration parameters";
  private final ConfigurationClient client;
  private final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  private final ExportConfigValidatorResolver exportConfigValidatorResolver;
  private final ObjectMapper objectMapper;

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    log.info("Putting {} {}.", configId, exportConfig);
    validateIncomingExportConfig(exportConfig);
    var config = createConfigModel(exportConfig);
    client.putConfiguration(config, configId);
    log.info("Put {} {}.", configId, config);
  }

  @Override
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    log.info("Posting {}.", exportConfig);
    validateIncomingExportConfig(exportConfig);
    ModelConfiguration config = client.postConfiguration(createConfigModel(exportConfig));
    log.info("Posted {}.", config);
    return config;
  }

  @Override
  public ExportConfigCollection getConfigCollection(String query) {
    if (query == null) {
      return getFirstConfig().map(this::createExportConfigCollection).orElse(emptyExportConfigCollection());
    }
    ConfigurationCollection configurationCollection = client.getConfigurations(query);
    if (configurationCollection.getTotalRecords() > 0) {
      var exportConfigCollection = new ExportConfigCollection();
      configurationCollection.getConfigs().forEach(modelConfig -> exportConfigCollection
        .addConfigsItem(defaultModelConfigToExportConfigConverter.convert(modelConfig))
      );
      return exportConfigCollection.totalRecords(exportConfigCollection.getConfigs().size());
    }
    return new ExportConfigCollection().totalRecords(0);
  }

  @Override
  public Optional<ExportConfig> getFirstConfig() {
    var configurationCollection = client.getConfigurations(String.format(DEFAULT_CONFIG_QUERY, MODULE_NAME, DEFAULT_CONFIG_NAME));
    if (configurationCollection.getTotalRecords() == 0) {
      return Optional.empty();
    }
    var config = defaultModelConfigToExportConfigConverter.convert(configurationCollection.getConfigs().get(0));
    return Optional.of(config);
  }

  @SneakyThrows
  private ModelConfiguration createConfigModel(ExportConfig exportConfig) {
    var config = new ModelConfiguration();
    config.setModule(MODULE_NAME);
    config.setConfigName(DEFAULT_CONFIG_NAME);
    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(exportConfig));
    return config;
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

  protected void validateIncomingExportConfig(ExportConfig exportConfig) {
    exportConfigValidatorResolver.resolve(exportConfig.getType(), ExportTypeSpecificParameters.class).ifPresent(validator -> {
      Errors errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
      validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);
    });
  }
}
