package org.folio.des.service.config;

import java.util.Optional;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ModelConfiguration;

public interface ExportConfigService {

  void updateConfig(String configId, ExportConfig exportConfig);

  ModelConfiguration postConfig(ExportConfig exportConfig);

  ExportConfigCollection getConfigCollection(String query, Integer limit);

  Optional<ExportConfig> getFirstConfig();

  ExportConfig getConfigById(String configId);

  void deleteConfigById(String configId);

}
