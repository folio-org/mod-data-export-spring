package org.folio.des.service;

import org.folio.des.domain.dto.ConfigModel;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;

import java.util.Optional;

public interface ExportConfigService {

  void updateConfig(String configId, ExportConfig exportConfig);

  ConfigModel postConfig(ExportConfig exportConfig);

  ExportConfigCollection getConfigCollection();

  Optional<ExportConfig> getConfig();

}
