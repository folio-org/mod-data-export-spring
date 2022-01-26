package org.folio.des.repository.criteria;

import static org.folio.des.repository.criteria.CQL2JPACriteria.CRITERIA_JSONB_START;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.z3950.zing.cql.CQLTermNode;

public class JsonbNodeToPredicateConverter {

  public static final String JSONB_EXTRACT_PATH_TEXT_FUNC = "jsonb_extract_path_text";

  public Predicate convert(CQLTermNode node, CriteriaBuilder cb, Root<?> root) {
    List<String> fieldNames = getFieldNames(node.getIndex());
    int fieldsNumber = fieldNames.size();
    if (fieldsNumber > 0) {
      String rootFieldName = fieldNames.get(0);
      Expression<String> expression = cb.function(JSONB_EXTRACT_PATH_TEXT_FUNC, String.class, root.<String>get(rootFieldName));
      if (fieldsNumber == 2) {
        expression = cb.function(JSONB_EXTRACT_PATH_TEXT_FUNC, String.class, root.<String>get(rootFieldName),
          cb.literal(fieldNames.get(1)));
      } else if (fieldsNumber == 3) {
        expression = cb.function(JSONB_EXTRACT_PATH_TEXT_FUNC, String.class, root.<String>get(rootFieldName),
          cb.literal(fieldNames.get(1)), cb.literal(fieldNames.get(2)));
      } else if (fieldsNumber == 4) {
        expression = cb.function(JSONB_EXTRACT_PATH_TEXT_FUNC, String.class, root.<String>get(rootFieldName),
          cb.literal(fieldNames.get(1)), cb.literal(fieldNames.get(2)), cb.literal(fieldNames.get(3)));
      }
      return expression.in(node.getTerm());
    } else {
      throw new IllegalArgumentException(String.format("Wrong JSONB criteria: %s", node.getIndex()));
    }
  }

  @NotNull
  private List<String> getFieldNames(String jsonPath) {
    return Arrays.stream(jsonPath.split("\\."))
                                    .filter(fieldName -> !CRITERIA_JSONB_START.equals(fieldName))
                                    .collect(Collectors.toList());
  }

}
