package org.folio.des.service;

import java.util.Optional;
import org.folio.des.domain.dto.BursarExportConfig;
import org.folio.des.domain.dto.BursarExportConfigCollection;
import org.folio.des.domain.dto.bursar.ConfigModel;

public interface ConfigBursarExportService {

  void updateConfig(String configId, BursarExportConfig config);

  ConfigModel postConfig(BursarExportConfig bursarExportConfig);

  BursarExportConfigCollection getConfigCollection();

  Optional<BursarExportConfig> getConfig();
}
