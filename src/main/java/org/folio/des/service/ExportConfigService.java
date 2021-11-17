package org.folio.des.service;

import java.util.List;
import java.util.Optional;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ModelConfiguration;

public interface ExportConfigService {

  void updateConfig(String configId, ExportConfig exportConfig);

  ModelConfiguration postConfig(ExportConfig exportConfig);

  ExportConfigCollection getConfigCollection(Integer offset, Integer limit, String query);

  Optional<ExportConfig> getConfig();

  List<ExportConfig> getConfigs(String query);

}
