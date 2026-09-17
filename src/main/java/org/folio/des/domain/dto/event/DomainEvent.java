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

  /**
   * Builds a {@code CREATE} event carrying only the post-change snapshot.
   *
   * @param newValue the newly-created snapshot
   * @param tenant   the tenant the change happened in
   * @return a populated {@code CREATE} domain event
   */
  public static <T> DomainEvent<T> createEvent(T newValue, String tenant) {
    return DomainEvent.<T>builder()
      .eventId(UUID.randomUUID())
      .eventTs(System.currentTimeMillis())
      .tenant(tenant)
      .type(DomainEventType.CREATE)
      .newValue(newValue)
      .build();
  }

  /**
   * Builds an {@code UPDATE} event carrying both the pre- and post-change snapshots.
   *
   * @param oldValue the pre-change snapshot
   * @param newValue the post-change snapshot
   * @param tenant   the tenant the change happened in
   * @return a populated {@code UPDATE} domain event
   */
  public static <T> DomainEvent<T> updateEvent(T oldValue, T newValue, String tenant) {
    return DomainEvent.<T>builder()
      .eventId(UUID.randomUUID())
      .eventTs(System.currentTimeMillis())
      .tenant(tenant)
      .type(DomainEventType.UPDATE)
      .oldValue(oldValue)
      .newValue(newValue)
      .build();
  }
}

