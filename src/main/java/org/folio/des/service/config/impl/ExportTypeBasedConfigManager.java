package org.folio.des.service.config.impl;

import java.util.Optional;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.config.ExportConfigService;
import org.folio.spring.exception.NotFoundException;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Log4j2
@Service
public class ExportTypeBasedConfigManager {
  public static final String EXPORT_CONFIGURATION_NOT_FOUND = "Export configuration not found or parse error : %s";

  private final ConfigurationClient client;
  private final ExportConfigServiceResolver exportConfigServiceResolver;
  private final ExportConfigService defaultExportConfig;
  private final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;

  public void updateConfig(String configId, ExportConfig exportConfig) {
    exportConfigServiceResolver.resolve(exportConfig.getType())
            .ifPresentOrElse(service -> service.updateConfig(configId, exportConfig),
              () -> defaultExportConfig.updateConfig(configId, exportConfig));
  }

  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    Optional<ExportConfigService> exportConfigService = exportConfigServiceResolver.resolve(exportConfig.getType());
    if (exportConfigService.isPresent()) {
      return exportConfigService.get().postConfig(exportConfig);
    }
    return defaultExportConfig.postConfig(exportConfig);
  }

  public ExportConfigCollection getConfigCollection(ExportType exportType, String query) {
    Optional<ExportConfigService> exportConfigService = exportConfigServiceResolver.resolve(exportType);
    if (exportConfigService.isPresent()) {
      return exportConfigService.get().getConfigCollection(query);
    }
    return defaultExportConfig.getConfigCollection(query);
  }

  public ExportConfig getConfigById(String exportConfigId) {
    var configuration = client.getConfigById(exportConfigId);

    if (configuration == null) {
      throw new NotFoundException(String.format(EXPORT_CONFIGURATION_NOT_FOUND, exportConfigId));
    }
    return defaultModelConfigToExportConfigConverter.convert(configuration);
  }

}
