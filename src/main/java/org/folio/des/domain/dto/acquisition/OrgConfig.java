package org.folio.des.domain.dto.acquisition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgConfig {
  @JsonProperty("locale")
  private String locale;
  @JsonProperty("timezone")
  private String timezone;
  @JsonProperty("currency")
  private String currency;
}
