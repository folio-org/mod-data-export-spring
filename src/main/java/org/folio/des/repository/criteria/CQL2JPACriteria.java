package org.folio.des.repository.criteria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.cql2pgjson.exception.CQLFeatureUnsupportedException;
import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.cql2pgjson.model.CqlModifiers;
import org.folio.cql2pgjson.model.CqlSort;
import org.folio.cql2pgjson.model.CqlTermFormat;
import org.folio.cql2pgjson.util.Cql2SqlUtil;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.ModifierSet;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CQL2JPACriteria<E> {

  private final CriteriaBuilder builder;
  private final JsonbNodeConverter jsonbNodeConverter;
  public final Root<E> root;
  public static final String NOT_EQUALS_OPERATOR = "<>";
  public final CriteriaQuery<E> criteria;
  private static final String ASTERISKS_SIGN = "*";
  public static final String CRITERIA_JSONB_START = "jsonb";

  public CQL2JPACriteria(Class<E> entityCls, EntityManager entityManager) {
    this.builder = entityManager.getCriteriaBuilder();
    this.jsonbNodeConverter = new JsonbNodeConverter();
    criteria = builder.createQuery(entityCls);
    root = criteria.from(entityCls);
  }

  /**
   * Convert the CQL query into a SQL query and return the WHERE and the ORDER BY clause.
   *
   * @param cql the query to convert
   * @return SQL query
   */
  public CriteriaQuery<E> toCriteria(String cql) throws QueryValidationException {
    try {
      CQLParser parser = new CQLParser();
      CQLNode node = parser.parse(cql);
      return toCriteria(node);
    } catch (IOException | CQLParseException e) {
      throw new QueryValidationException(e);
    }
  }

  private CriteriaQuery<E> toCriteria(CQLNode node)
    throws QueryValidationException {
    Predicate predicates;

    if (node instanceof CQLSortNode cqlSortNode) {
      processSort(cqlSortNode);
      predicates = process(cqlSortNode.getSubtree());
    } else {
      predicates = process(node);
    }

    return criteria.where(predicates);
  }

  private void processSort(CQLSortNode node) throws CQLFeatureUnsupportedException {
    List<Order> orders = new ArrayList<>();

    for (ModifierSet sortIndex : node.getSortIndexes()) {
      final CqlModifiers modifiers = new CqlModifiers(sortIndex);
      String jsonPath = sortIndex.getBase();
      int fieldNamesSize = JsonbNodeConverter.getFieldNames(jsonPath).size();

      Expression<String> field = fieldNamesSize > 1 ?
        jsonbNodeConverter.convertToExpression(root, jsonPath, builder) : root.get(jsonPath);

      orders.add(getOrder(field, modifiers));
    }

    criteria.orderBy(orders);
  }

  private Order getOrder(Expression<String> field, CqlModifiers modifiers) {
    return CqlSort.DESCENDING.equals(modifiers.getCqlSort())
      ? builder.desc(field)
      : builder.asc(field);
  }

  private Predicate process(CQLNode node) throws QueryValidationException {
    if (node instanceof CQLTermNode cqlTermNode) {
      return processTerm(cqlTermNode);
    }
    if (node instanceof CQLBooleanNode cqlBooleanNode) {
      return processBoolean(cqlBooleanNode);
    }
    throw createUnsupportedException(node);
  }

  private static CQLFeatureUnsupportedException createUnsupportedException(CQLNode node) {
    return new CQLFeatureUnsupportedException("Not implemented yet: " + node.getClass().getName());
  }

  private Predicate processBoolean(CQLBooleanNode node) throws QueryValidationException {
    if (node instanceof CQLAndNode) {
      return builder.and(process(node.getLeftOperand()), process(node.getRightOperand()));
    } else if (node instanceof CQLOrNode) {
      if (node.getRightOperand().getClass() == CQLTermNode.class) {
        // special case for the query the UI uses most often, before the user has
        // typed in anything: title=* OR contributors*= OR identifier=*
        CQLTermNode r = (CQLTermNode) (node.getRightOperand());
        if (ASTERISKS_SIGN.equals(r.getTerm()) && "=".equals(r.getRelation().getBase())) {
          log.debug("pgFT(): Simplifying =* OR =* ");
          return process(node.getLeftOperand());
        }
      }
      return builder.or(process(node.getLeftOperand()), process(node.getRightOperand()));
    } else if (node instanceof CQLNotNode) {
      return builder.not(
          builder.and(process(node.getLeftOperand()), process(node.getRightOperand())));
    } else {
      throw createUnsupportedException(node);
    }
  }

  private Predicate processTerm(CQLTermNode node) throws QueryValidationException {
    String fieldName = node.getIndex();
    if ("cql.allRecords".equalsIgnoreCase(fieldName)) {
      return builder.and();
    } else if (StringUtils.startsWithIgnoreCase(fieldName, CRITERIA_JSONB_START)) {
      return jsonbNodeConverter.convertToPredicate(node, builder, root);
    }

    var field = getPath(fieldName);
    CqlModifiers cqlModifiers = new CqlModifiers(node);
    return indexNode(field, node, cqlModifiers);
  }

  private Path<?> getPath(String fieldName) {
    if (fieldName.contains(".")) {
      final int dotIdx = fieldName.indexOf(".");
      final String attributeName = fieldName.substring(0, dotIdx);
      Join<E, Object> children = root.join(attributeName, JoinType.LEFT);
      root.fetch(attributeName);
      return children.get(fieldName.substring(dotIdx + 1));
    } else {
      return root.get(fieldName);
    }
  }

  private <E extends Comparable<? super E>> Predicate toPredicate(
      Expression<E> field, E value, String comparator) throws QueryValidationException {

    return switch (comparator) {
      case ">" -> builder.greaterThan(field, value);
      case "<" -> builder.lessThan(field, value);
      case ">=" -> builder.greaterThanOrEqualTo(field, value);
      case "<=" -> builder.lessThanOrEqualTo(field, value);
      case "==", "=" -> builder.equal(field, value);
      case NOT_EQUALS_OPERATOR -> builder.notEqual(field, value);
      default -> throw new QueryValidationException(
        "CQL: Unsupported operator '"
          + comparator
          + "', "
          + " only supports '=', '==', and '<>' (possibly with right truncation)");
    };
  }

  private Predicate indexNode(Path<?> field, CQLTermNode node, CqlModifiers modifiers)
      throws QueryValidationException {

    boolean isString = String.class.equals(field.getJavaType());

    String comparator = node.getRelation().getBase().toLowerCase();

    switch (comparator) {
      case "=":
        if (CqlTermFormat.NUMBER.equals(modifiers.getCqlTermFormat())) {
          return queryBySql(field, node, comparator);
        }
      case "adj", "all", "any", "==", NOT_EQUALS_OPERATOR:
        return buildQuery(field, node, isString, comparator);
      case "<", ">", "<=", ">=":
        return queryBySql(field, node, comparator);
      default:
        throw new CQLFeatureUnsupportedException(
            "Relation " + comparator + " not implemented yet: " + node);
    }
  }

  private Predicate buildQuery(Path<?> field, CQLTermNode node, boolean isString, String comparator)
    throws QueryValidationException {
    if (isString) {
      return queryByLike((Path<String>) field, node, comparator);
    } else {
      return queryBySql(field, node, comparator);
    }
  }

  /** Create an SQL expression using LIKE query syntax. */
  private Predicate queryByLike(Path<String> field, CQLTermNode node, String comparator) {

    if (NOT_EQUALS_OPERATOR.equals(comparator)) {
      return builder.notLike(field, Cql2SqlUtil.cql2like(node.getTerm()));
    } else {
      return builder.like(field, Cql2SqlUtil.cql2like(node.getTerm()));
    }
  }

  /** Create an SQL expression using SQL as is syntax. */
  private Predicate queryBySql(Expression field, CQLTermNode node, String comparator)
      throws QueryValidationException {

    Comparable val = node.getTerm();

    Class<?> javaType = field.getJavaType();
    if (Number.class.equals(javaType)) {
      val = Integer.parseInt((String) val);
    } else if (UUID.class.equals(javaType)) {
      val = UUID.fromString((String) val);
    } else if (Boolean.class.equals(javaType)) {
      val = Boolean.valueOf((String) val);
    } else if (Date.class.equals(javaType)) {
      LocalDateTime dateTime = LocalDateTime.parse((String) val);
      val = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    } else if (javaType.isEnum()) {
      field = field.as(String.class);
    }

    return toPredicate(field, val, comparator);
  }
}
