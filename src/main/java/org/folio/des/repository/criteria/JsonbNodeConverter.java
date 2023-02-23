package org.folio.des.repository.criteria;

import static org.folio.des.repository.criteria.CQL2JPACriteria.CRITERIA_JSONB_START;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.List;

import org.z3950.zing.cql.CQLTermNode;

public class JsonbNodeConverter {

  public static final String JSONB_EXTRACT_PATH_TEXT_FUNC = "jsonb_extract_path_text";

  public static Expression<String> convertToExpression(Root<?> root, String jsonPath, CriteriaBuilder cb){
    List<String> fieldNames = getFieldNames(jsonPath);
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
    return expression;
    } else {
      throw new IllegalArgumentException(String.format("Wrong JSONB criteria: %s", jsonPath));
    }
  }
  public Predicate convertToPredicate(CQLTermNode node, CriteriaBuilder cb, Root<?> root) {
    String jsonPath = node.getIndex();
    Expression<String> expression = convertToExpression(root, jsonPath, cb);
    return expression.in(node.getTerm());
  }

  @NotNull
  private static List<String> getFieldNames(String jsonPath) {
    return Arrays.stream(jsonPath.split("\\."))
      .filter(fieldName -> !CRITERIA_JSONB_START.equals(fieldName))
      .toList();
  }

}
