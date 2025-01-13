package org.folio.des.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FilterOperator {
  OR_OPERATOR(" OR "),
  AND_OPERATOR(" AND ");

  private final String value;
}
