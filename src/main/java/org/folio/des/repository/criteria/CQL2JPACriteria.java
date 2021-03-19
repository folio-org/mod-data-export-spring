package org.folio.des.repository.criteria;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
public class CQL2JPACriteria<E> {

  private final Pattern uuidPattern =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private final CriteriaBuilder builder;

  public final Root<E> root;
  public static final String NOT_EQUALS_OPERATOR = "<>";
  private final Class<E> entityCls;
  public final CriteriaQuery<E> criteria;
  private static final String ASTERISKS_SIGN = "*";

  public CQL2JPACriteria(Class<E> entityCls, EntityManager entityManager) {
    this.builder = entityManager.getCriteriaBuilder();
    this.entityCls = entityCls;
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
      return toCriteria(node, entityCls);
    } catch (IOException | CQLParseException e) {
      throw new QueryValidationException(e);
    }
  }

  private CriteriaQuery<E> toCriteria(CQLNode node, Class<E> entityCls)
      throws QueryValidationException {
    Predicate predicates;

    if (node instanceof CQLSortNode) {
      CQLSortNode sortNode = (CQLSortNode) node;
      processSort(sortNode);
      predicates = process(sortNode.getSubtree());
    } else {
      predicates = process(node);
    }

    return criteria.where(predicates);
  }

  private void processSort(CQLSortNode node) throws CQLFeatureUnsupportedException {
    List<Order> orders = new ArrayList<>();
    for (ModifierSet sortIndex : node.getSortIndexes()) {
      final CqlModifiers modifiers = new CqlModifiers(sortIndex);
      orders.add(
          CqlSort.DESCENDING.equals(modifiers.getCqlSort())
              ? builder.desc(root.get(sortIndex.getBase()))
              : builder.asc(root.get(sortIndex.getBase())));
    }
    criteria.orderBy(orders);
  }

  private Predicate process(CQLNode node) throws QueryValidationException {
    if (node instanceof CQLTermNode) {
      return processTerm((CQLTermNode) node);
    }
    if (node instanceof CQLBooleanNode) {
      return processBoolean((CQLBooleanNode) node);
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
    }

    Path field;
    if (fieldName.contains(".")) {
      final int dotIdx = fieldName.indexOf(".");
      final String attributeName = fieldName.substring(0, dotIdx);
      Join<E, Object> children = root.join(attributeName, JoinType.LEFT);
      root.fetch(attributeName);
      field = children.get(fieldName.substring(dotIdx + 1));
    } else {
      field = root.get(fieldName);
    }
    CqlModifiers cqlModifiers = new CqlModifiers(node);
    return indexNode(field, node, cqlModifiers);
  }

  /**
   * Search a UUID field that we've extracted from the jsonb into a proper UUID database table
   * column. This is either the primary key id or a foreign key. There always exists an index. Using
   * BETWEEN lo AND hi with UUIDs is faster than a string comparison with truncation.
   *
   * @param node the CQL to convert into SQL
   * @return SQL where clause component for this term
   * @throws QueryValidationException on invalid UUID format or invalid operator
   */
  private Predicate processId(CQLTermNode node, Path field) throws QueryValidationException {
    String comparator = StringUtils.defaultString(node.getRelation().getBase());
    if (!node.getRelation().getModifiers().isEmpty()) {
      throw new QueryValidationException(
          "CQL: Unsupported modifier " + node.getRelation().getModifiers().get(0).getType());
    }
    String term = node.getTerm();
    if (!isValidUUID(term)) {
      throw new QueryValidationException("CQL: Invalid UUID after '" + comparator + "': " + term);
    }

    return toPredicate(field, UUID.fromString(term), comparator);
  }

  private <E extends Comparable<? super E>> Predicate toPredicate(
      Expression<E> field, E value, String comparator) throws QueryValidationException {

    switch (comparator) {
      case ">":
        return builder.greaterThan(field, value);
      case "<":
        return builder.lessThan(field, value);
      case ">=":
        return builder.greaterThanOrEqualTo(field, value);
      case "<=":
        return builder.lessThanOrEqualTo(field, value);
      case "==":
      case "=":
        return builder.equal(field, value);
      case NOT_EQUALS_OPERATOR:
        return builder.notEqual(field, value);
      default:
        throw new QueryValidationException(
            "CQL: Unsupported operator '"
                + comparator
                + "', "
                + " only supports '=', '==', and '<>' (possibly with right truncation)");
    }
  }

  private boolean isValidUUID(String term) {
    return uuidPattern.matcher(term).matches();
  }

  private Predicate indexNode(Path field, CQLTermNode node, CqlModifiers modifiers)
      throws QueryValidationException {

    if ("id".equals(field.getAlias()) || field.equals(root.getModel().getId(Object.class))) {
      return processId(node, field);
    }

    boolean isString = String.class.equals(field.getJavaType());

    String comparator = node.getRelation().getBase().toLowerCase();

    switch (comparator) {
      case "=":
        if (CqlTermFormat.NUMBER.equals(modifiers.getCqlTermFormat())) {
          return queryBySql(field, node, comparator);
        }
      case "adj":
      case "all":
      case "any":
        if (isString) {
          return queryByLike(field, node, comparator);
        } else {
          return queryBySql(field, node, comparator);
        }
      case "==":
      case NOT_EQUALS_OPERATOR:
        if (isString) {
          return queryByLike(field, node, comparator);
        } else {
          return queryBySql(field, node, comparator);
        }
      case "<":
      case ">":
      case "<=":
      case ">=":
        return queryBySql(field, node, comparator);
      default:
        throw new CQLFeatureUnsupportedException(
            "Relation " + comparator + " not implemented yet: " + node.toString());
    }
  }

  /** Create an SQL expression using LIKE query syntax. */
  private Predicate queryByLike(Path field, CQLTermNode node, String comparator) {

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

    Class javaType = field.getJavaType();
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
