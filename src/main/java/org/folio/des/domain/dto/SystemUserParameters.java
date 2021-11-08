package org.folio.des.domain.dto;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemUserParameters {

  @Id
  @JsonIgnore
  private UUID id;

  private String username;

  private String password;

  @JsonIgnore
  private String okapiToken;

  @JsonIgnore
  private String okapiUrl;

  @JsonIgnore
  private String tenantId;
}
