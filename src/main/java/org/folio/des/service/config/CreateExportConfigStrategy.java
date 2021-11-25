package org.folio.des.service.config;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;

public interface CreateExportConfigStrategy {
  ModelConfiguration postConfig(ExportConfig exportConfig);
}
