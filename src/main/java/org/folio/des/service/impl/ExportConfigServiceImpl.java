package org.folio.des.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.ExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
@Service
public class ExportConfigServiceImpl implements ExportConfigService {

  public static final String CONFIG_QUERY = "module==%s and configName==%s";
  public static final String MODULE_NAME = "mod-data-export-spring";
  public static final String CONFIG_NAME = "export_config_parameters";

  private final ConfigurationClient client;
  private final ObjectMapper objectMapper;
  private final ExportConfigValidatorResolver exportConfigValidatorResolver;
  private final ExportConfigConverterResolver exportConfigConverterResolver;

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    log.info("Putting {} {}.", configId, exportConfig);
    validateIncomingExportConfig(exportConfig);
    ModelConfiguration modelConfig = buildModelConfiguration(exportConfig);
    client.putConfiguration(modelConfig, configId);
    log.info("Put {} {}.", configId, modelConfig);
  }

  @Override
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    log.info("Posting {}.", exportConfig);
    validateIncomingExportConfig(exportConfig);
    ModelConfiguration modelConfig = buildModelConfiguration(exportConfig);
    ModelConfiguration createdModelConfig = client.postConfiguration(modelConfig);
    log.info("Posted {}.", createdModelConfig);
    return createdModelConfig;
  }

  @Override
  public ExportConfigCollection getConfigCollection(Integer offset, Integer limit, String query) {
    if (StringUtils.isEmpty(query)) {
      return getConfig().map(this::createExportConfigCollection).orElse(emptyExportConfigCollection());
    } else {
      List<ExportConfig> exportConfigs = getConfigs(query);

      ExportConfigCollection configCollection = new ExportConfigCollection();
      configCollection.configs(exportConfigs);
      configCollection.totalRecords(exportConfigs.size());
      return configCollection;
    }
  }

  @Override
  public List<ExportConfig> getConfigs(String query) {
    ConfigurationCollection configurationCollection;

    if (query == null) {
      return getConfig().map(this::createExportConfigCollection).orElse(emptyExportConfigCollection()).getConfigs();
    } else if (query.contains("type")) {
      query = query.substring(query.lastIndexOf("==") + 2);
      query = String.format(CONFIG_QUERY, MODULE_NAME, (CONFIG_NAME + "-" + query.toLowerCase()));
      configurationCollection = client.getConfiguration(query);
    } else {
      configurationCollection = client.getConfiguration(query);
    }

    if (configurationCollection.getTotalRecords() == 0) {
      return Collections.emptyList();
    }

    List<ExportConfig> exportConfigs = new ArrayList<>();

    configurationCollection.getConfigs().forEach(item -> {
      try {
        exportConfigs.add(parseExportConfig(item));
      } catch (JsonProcessingException e) {
        log.error("Can not parse configuration for module {} with config name {}", item.getModule(), item.getConfigName());
      }
    });

    return exportConfigs;
  }

  @Override
  public Optional<ExportConfig> getConfig() {
    var configurationCollection = client.getConfiguration(String.format(CONFIG_QUERY, MODULE_NAME, CONFIG_NAME));

    if (configurationCollection.getTotalRecords() == 0) {
      return Optional.empty();
    }

    try {
      var config = parseExportConfig(configurationCollection);
      return Optional.of(config);
    } catch (JsonProcessingException e) {
      log.error("Can not parse configuration for module {} with config name {}", MODULE_NAME, CONFIG_NAME);
      return Optional.empty();
    }
  }

  protected void validateIncomingExportConfig(ExportConfig exportConfig) {
    Errors errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
    exportConfigValidatorResolver.resolve(exportConfig.getType(), ExportTypeSpecificParameters.class)
                        .ifPresent(validator -> {
                          validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);
                        });
  }

  protected ModelConfiguration buildModelConfiguration(ExportConfig exportConfig) {
    var exportConfigConverter = exportConfigConverterResolver.resolve(exportConfig.getType());
    return exportConfigConverter.convert(exportConfig);
  }

  private ExportConfig parseExportConfig(ModelConfiguration modelConfiguration) throws com.fasterxml.jackson.core.JsonProcessingException {
    final String value = modelConfiguration.getValue();
    var config = objectMapper.readValue(value, ExportConfig.class);
    config.setId(modelConfiguration.getId());
    return config;
  }

  private ExportConfig parseExportConfig(ConfigurationCollection configurationCollection) throws com.fasterxml.jackson.core.JsonProcessingException {
    var configs = configurationCollection.getConfigs().get(0);
    final String value = configs.getValue();
    var config = objectMapper.readValue(value, ExportConfig.class);
    config.setId(configs.getId());
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
}
