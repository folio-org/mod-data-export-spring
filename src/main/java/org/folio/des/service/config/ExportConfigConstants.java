package org.folio.des.service.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExportConfigConstants {
  public static final String DEFAULT_MODULE_NAME = "mod-data-export-spring";
  public static final String DEFAULT_MODULE_QUERY = "module==" + DEFAULT_MODULE_NAME;
  public static final String DEFAULT_CONFIG_QUERY = DEFAULT_MODULE_QUERY + " and configName==%s";
  public static final String DEFAULT_CONFIG_NAME = "export_config_parameters";
}
