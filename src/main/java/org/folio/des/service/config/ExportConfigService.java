package org.folio.des.service.config;

import java.util.Optional;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;

public interface ExportConfigService {

  void updateConfig(String configId, ExportConfig exportConfig);

  ExportConfig postConfig(ExportConfig exportConfig);

  ExportConfigCollection getConfigCollection(String query, Integer limit);

  Optional<ExportConfig> getFirstConfig();

  ExportConfig getConfigById(String configId);

  void deleteConfigById(String configId);

}
