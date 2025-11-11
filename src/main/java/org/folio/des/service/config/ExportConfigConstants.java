package org.folio.des.service.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExportConfigConstants {
  public static final String DEFAULT_CONFIG_QUERY = "configName==%s";
  public static final String DEFAULT_CONFIG_NAME = "export_config_parameters";
}
