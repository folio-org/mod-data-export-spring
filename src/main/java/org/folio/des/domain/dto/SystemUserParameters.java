package org.folio.des.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemUserParameters {

  @JsonIgnore
  private UUID id;

  private String username;

  private String password;

  @JsonIgnore
  private String okapiToken;

  @JsonIgnore
  private String okapiUrl;

  @JsonIgnore
  private String tenant;

  @JsonIgnore
  private String userId;
}
