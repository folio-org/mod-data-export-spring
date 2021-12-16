package org.folio.des.service.config;

import static org.folio.des.util.BulkEditConstants.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.folio.des.util.BulkEditConstants.CONFIG_NAME;
import static org.folio.des.util.BulkEditConstants.DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD;
import static org.folio.des.util.BulkEditConstants.EXPIRATION_PERIOD_PARAMETER;
import static org.folio.des.util.BulkEditConstants.JOB_EXPIRATION_PERIOD;
import static org.folio.des.util.BulkEditConstants.MODULE_NAME;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ModelConfiguration;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkEditConfigService {
  private final ConfigurationClient configurationClient;

  public ConfigurationCollection getBulkEditConfigurations() {
    return configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, CONFIG_NAME));
  }

  @SneakyThrows
  public int getBulkEditJobExpirationPeriod() {
    var configs = getBulkEditConfigurations();
    if (!configs.getConfigs().isEmpty()) {
      var config = configs.getConfigs().get(0);
      log.info("Found bulk-edit configuration: {}", config);
      var bulkEditConfig = new ObjectMapper().readValue(config.getValue(), new TypeReference<Map<String,String>>(){});
      return Integer.parseInt(bulkEditConfig.getOrDefault(JOB_EXPIRATION_PERIOD, DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD));
    }
    return Integer.parseInt(DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD);
  }

  public void checkBulkEditConfiguration() {
    if (getBulkEditConfigurations().getConfigs().isEmpty()) {
      log.info("Bulk-edit configuration was not found, uploading default");
      configurationClient.postConfiguration(buildDefaultConfig());
    }
  }

  @SneakyThrows
  public static ModelConfiguration buildDefaultConfig() {
    var configMap = Collections.singletonMap(EXPIRATION_PERIOD_PARAMETER, DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD);
    return new ModelConfiguration()
      .module(MODULE_NAME)
      .configName(CONFIG_NAME)
      ._default(true)
      .enabled(true)
      .value(new ObjectMapper().writeValueAsString(configMap));
  }
}
