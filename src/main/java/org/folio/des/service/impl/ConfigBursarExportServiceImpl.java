package org.folio.des.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.domain.dto.BursarExportConfig;
import org.folio.des.domain.dto.BursarExportConfigCollection;
import org.folio.des.domain.dto.bursar.ConfigModel;
import org.folio.des.service.ConfigBursarExportService;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Log4j2
@Service
public class ConfigBursarExportServiceImpl implements ConfigBursarExportService {

  private static final String CONFIG_QUERY = "module==%s and configName==%s";
  private static final String MODULE_NAME = "mod-bursar-export";
  private static final String CONFIG_NAME = "schedule_parameters";
  private final ConfigurationClient client;
  private final ObjectMapper objectMapper;

  @Override
  public void updateConfig(String configId, BursarExportConfig scheduleConfig) {
    ConfigModel config = createConfigModel(scheduleConfig);
    client.putConfiguration(config, configId);
  }

  @Override
  public ConfigModel postConfig(BursarExportConfig bursarExportConfig) {
    ConfigModel config = createConfigModel(bursarExportConfig);
    return client.postConfiguration(config);
  }

  @SneakyThrows
  private ConfigModel createConfigModel(BursarExportConfig scheduleConfiguration) {
    var config = new ConfigModel();
    config.setModule(MODULE_NAME);
    config.setConfigName(CONFIG_NAME);
    config.setDescription("Parameters to schedule the job");
    config.setEnabled(true);
    config.setDefaultFlag(true);
    config.setValue(objectMapper.writeValueAsString(scheduleConfiguration));
    return config;
  }

  @SneakyThrows
  @Override
  public BursarExportConfigCollection getConfigCollection() {
    return getConfig().map(this::createConfigCollection).orElse(emptyConfigCollection());
  }

  @Override
  public Optional<BursarExportConfig> getConfig() {
    final String configuration =
        client.getConfiguration(String.format(CONFIG_QUERY, MODULE_NAME, CONFIG_NAME));

    final JSONObject jsonObject = new JSONObject(configuration);
    if (jsonObject.getInt("totalRecords") == 0) {
      return Optional.empty();
    }

    try {
      var config = parseScheduleConfig(jsonObject);
      return Optional.of(config);
    } catch (JsonProcessingException e) {
      log.error(
          "Can not parse configuration for module {} with config name {}",
          MODULE_NAME,
          CONFIG_NAME);
      return Optional.empty();
    }
  }

  private BursarExportConfig parseScheduleConfig(JSONObject jsonObject)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    final JSONObject configs = jsonObject.getJSONArray("configs").getJSONObject(0);
    final ConfigModel configModel = objectMapper.readValue(configs.toString(), ConfigModel.class);
    final String value = configModel.getValue();
    var config = objectMapper.readValue(value, BursarExportConfig.class);
    config.setId(configModel.getId());
    return config;
  }

  private BursarExportConfigCollection createConfigCollection(BursarExportConfig scheduleConfig) {
    var configCollection = new BursarExportConfigCollection();
    configCollection.addConfigsItem(scheduleConfig);
    configCollection.setTotalRecords(1);
    return configCollection;
  }

  private BursarExportConfigCollection emptyConfigCollection() {
    var configCollection = new BursarExportConfigCollection();
    configCollection.setTotalRecords(0);
    return configCollection;
  }
}
