package org.folio.des.service.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExportConfigConstants {
  public final static String DEFAULT_CONFIG_QUERY = "module==%s and configName==%s";
  public final static String MODULE_NAME = "mod-data-export-spring";
  public final static String DEFAULT_CONFIG_NAME = "export_config_parameters";
}
