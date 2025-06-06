package org.folio.des.service.config.impl;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class BaseExportConfigService implements ExportConfigService {

  protected final ConfigurationClient client;
  protected final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  protected final ExportConfigConverterResolver exportConfigConverterResolver;
  protected final ExportConfigValidatorResolver exportConfigValidatorResolver;

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
    var preparedConfig = createConfigModel(exportConfig);
    ModelConfiguration config = client.postConfiguration(preparedConfig);
    log.info("Posted {}.", config);
    return config;
  }

  @Override
  public ExportConfigCollection getConfigCollection(String query, Integer limit) {
    log.info("getConfigCollection:: by query={} with limit={}.", query, limit);
    ConfigurationCollection configurationCollection = client.getConfigurations(query, limit);
    if (configurationCollection.getTotalRecords() > 0) {
      var exportConfigCollection = new ExportConfigCollection();
      configurationCollection.getConfigs().forEach(modelConfig -> exportConfigCollection
        .addConfigsItem(defaultModelConfigToExportConfigConverter.convert(modelConfig))
      );
      ExportConfigCollection totalRecords = exportConfigCollection.totalRecords(exportConfigCollection.getConfigs().size());
      log.info("getConfigCollection:: totalRecords={}.", totalRecords);
      return totalRecords;
    }
    log.debug("getConfigCollection:: returned empty result set.");
    return new ExportConfigCollection().totalRecords(0);
  }

  @Override
  public Optional<ExportConfig> getFirstConfig() {
    var configurationCollection = getConfigCollection(StringUtils.EMPTY, 1);
    if (configurationCollection.getTotalRecords() == 0) {
      return Optional.empty();
    }
    return Optional.of(configurationCollection.getConfigs().get(0));
  }

  @SneakyThrows
  protected ModelConfiguration createConfigModel(ExportConfig exportConfig) {
    var converter = exportConfigConverterResolver.resolve(exportConfig.getType());
    return converter.convert(exportConfig);
  }

  protected void validateIncomingExportConfig(ExportConfig exportConfig) {
    exportConfigValidatorResolver.resolve(exportConfig.getType(), ExportTypeSpecificParameters.class).ifPresent(validator -> {
      Errors errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
      validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);
    });
  }
}
