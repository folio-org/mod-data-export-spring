package org.folio.des.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.ExportConfigService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Log4j2
@Service
public class ExportConfigServiceImpl implements ExportConfigService {

  private static final String CONFIG_QUERY = "module==%s and configName==%s";
  private static final String MODULE_NAME = "mod-data-export-spring";
  private static final String CONFIG_NAME = "export_config_parameters";
  private static final String CONFIG_DESCRIPTION = "Data export configuration parameters";
  private final ConfigurationClient client;
  private final ObjectMapper objectMapper;

  public static void checkConfig(ExportType type, ExportTypeSpecificParameters exportTypeSpecificParameters) {
    if (type == ExportType.BURSAR_FEES_FINES && exportTypeSpecificParameters.getBursarFeeFines() == null) {
      throw new IllegalArgumentException(
          String.format("%s type should contain %s parameters", type, BursarFeeFines.class.getSimpleName()));
    }
  }

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    log.info("Putting {} {}.", configId, exportConfig);
    checkConfig(exportConfig.getType(), exportConfig.getExportTypeSpecificParameters());
    var config = createConfigModel(exportConfig);
    client.putConfiguration(config, configId);
    log.info("Put {} {}.", configId, config);
  }

  @Override
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    log.info("Posting {}.", exportConfig);
    checkConfig(exportConfig.getType(), exportConfig.getExportTypeSpecificParameters());
    ModelConfiguration config = client.postConfiguration(createConfigModel(exportConfig));
    log.info("Posted {}.", config);
    return config;
  }

  @SneakyThrows
  private ModelConfiguration createConfigModel(ExportConfig exportConfig) {
    var config = new ModelConfiguration();
    config.setModule(MODULE_NAME);
    config.setConfigName(CONFIG_NAME);
    config.setDescription(CONFIG_DESCRIPTION);
    config.setEnabled(true);
    config.setDefault(true);
    config.setValue(objectMapper.writeValueAsString(exportConfig));
    return config;
  }

  @Override
  public ExportConfigCollection getConfigCollection() {
    return getConfig().map(this::createExportConfigCollection).orElse(emptyExportConfigCollection());
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
