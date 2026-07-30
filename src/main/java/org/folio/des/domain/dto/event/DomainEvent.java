package org.folio.des.domain.dto.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainEvent<T> {

  private UUID eventId;
  private long eventTs;
  private String tenant;
  private DomainEventType type;

  @JsonProperty("old")
  private T oldValue;

  @JsonProperty("new")
  private T newValue;
}

