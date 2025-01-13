package org.folio.des.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FilterPredicate {
  BY_TYPE_CONDITION("type=="),
  BY_VALUE_CONDITION("value==");

  private final String value;
}
