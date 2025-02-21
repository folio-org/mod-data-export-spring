package org.folio.des.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FilterOperator {
  OR_OPERATOR(" OR ", "(?i)( OR )"),
  AND_OPERATOR(" AND ", "(?i)( AND )");

  private final String value;
  private final String regexPattern;
}
