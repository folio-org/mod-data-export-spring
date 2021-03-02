package org.folio.des.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConfigModel {

  private String id;
  private String module;
  private String configName;
  private String description;
  @JsonProperty("default")
  private boolean defaultFlag;
  private boolean enabled;
  private String value;

}
