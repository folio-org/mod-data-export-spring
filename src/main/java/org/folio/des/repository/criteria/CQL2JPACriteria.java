package org.folio.des.repository.criteria;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
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
import org.folio.cql2pgjson.model.CqlAccents;
import org.folio.cql2pgjson.model.CqlCase;
import org.folio.cql2pgjson.model.CqlModifiers;
import org.folio.cql2pgjson.model.CqlSort;
import org.folio.cql2pgjson.model.CqlTermFormat;
import org.folio.cql2pgjson.util.Cql2PgUtil;
import org.folio.cql2pgjson.util.Cql2SqlUtil;
import org.folio.rest.persist.ddlgen.Index;
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

    if (node instanceof CQLSortNode) {
      List<Order> orders = new ArrayList<>();
      for (ModifierSet sortIndex : ((CQLSortNode) node).getSortIndexes()) {
        final CqlModifiers modifiers = new CqlModifiers(sortIndex);
        orders.add(
            CqlSort.DESCENDING.equals(modifiers.getCqlSort())
                ? builder.desc(root.get(sortIndex.getBase()))
                : builder.asc(root.get(sortIndex.getBase())));
      }
      criteria.orderBy(orders);
    }

    Predicate predicates = process(node);
    return criteria.where(predicates);
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

  /**
   * Return $term, lower($term), f_unaccent($term) or lower(f_unaccent($term)) according to the
   * cqlModifiers. If undefined use CqlAccents.IGNORE_ACCENTS and CqlCase.IGNORE_CASE as default.
   *
   * @param term the String to wrap
   * @param cqlModifiers what functions to use
   * @return wrapped term
   */
  private static String wrapInLowerUnaccent(String term, CqlModifiers cqlModifiers) {
    return Cql2PgUtil.wrapInLowerUnaccent(
        term,
        cqlModifiers.getCqlCase() != CqlCase.RESPECT_CASE,
        cqlModifiers.getCqlAccents() != CqlAccents.RESPECT_ACCENTS);
  }

  /**
   * Return $term, lower($term), f_unaccent($term) or lower(f_unaccent($term)) according to the
   * modifiers of index.
   *
   * @param term the String to wrap
   * @param index where to get the modifiers from
   * @return wrapped term
   */
  private static String wrapIndexExpression(String term, Index index) {
    if (index == null) {
      return Cql2PgUtil.wrapInLowerUnaccent(term, true, true);
    }
    return Cql2PgUtil.wrapInLowerUnaccent(term, !index.isCaseSensitive(), index.isRemoveAccents());
  }

  /**
   * Return $term, lower($term), f_unaccent($term), lower(f_unaccent($term)) or $term wrapped using
   * custom sqlExpressionQuery wrapper according to the modifiers of index.
   *
   * @param term the String to wrap
   * @param index where to get the modifiers from
   * @return wrapped term
   */
  private static String wrapQueryExpression(String term, Index index) {
    if (index == null) {
      return Cql2PgUtil.wrapInLowerUnaccent(term, true, true);
    }
    String wrapper = index.getSqlExpressionQuery();
    if (wrapper == null) {
      return Cql2PgUtil.wrapInLowerUnaccent(
          term, !index.isCaseSensitive(), index.isRemoveAccents());
    }
    return wrapper.replace("$", term);
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
      // CQL "NOT" means SQL "AND NOT", see section "7. Boolean Operators" in
      // https://www.loc.gov/standards/sru/cql/spec.html
      // TODO: manage case when the field does not exist.
      return builder.not(
          builder.and(process(node.getLeftOperand()), process(node.getRightOperand())));
    } else {
      throw createUnsupportedException(node);
    }
  }

  private Predicate processTerm(CQLTermNode node) throws QueryValidationException {
    String fieldName = node.getIndex();
    if ("cql.allRecords".equalsIgnoreCase(fieldName)) {
      // TODO: something like return builder.isTrue(true);
    }

    // TODO: create aliases if there is "." in the field name to search over linked tables

    if ("cql.serverChoice".equalsIgnoreCase(fieldName)) {
      // TODO: what is serverChoice?
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

    // TODO: consider removal or handle asterisks
    /*
    if (StringUtils.isEmpty(term)) {
      term = ASTERISKS_SIGN;
    }
    if (ASTERISKS_SIGN.equals(term) && "id".equals(columnName)) {
      return equals ? "true" : "false";  // no need to check
      // since id is a mandatory field, so
      // "all that have id" is the same as "all records"
    }
    if (!term.contains(ASTERISKS_SIGN)) { // exact match
      if (!isValidUUID(term)) {
        log.warn("Invalid UUID: " + term);
      }
      return equals ?  : ;
    }
    String truncTerm = term;
    while (truncTerm.endsWith(ASTERISKS_SIGN)) {  // remove trailing stars
      truncTerm = truncTerm.substring(0, truncTerm.length() - 1);
    }
    if (truncTerm.contains(ASTERISKS_SIGN)) { // any remaining '*' is an error
      throw new QueryValidationException("CQL: only right truncation supported for id:  " + term);
    }
    String lo = new StringBuilder("00000000-0000-0000-0000-000000000000")
        .replace(0, truncTerm.length(), truncTerm).toString();
    String hi = new StringBuilder("ffffffff-ffff-ffff-ffff-ffffffffffff")
        .replace(0, truncTerm.length(), truncTerm).toString();
    if (!isValidUUID(lo) || !isValidUUID(hi)) {
      log.warn("Invalid UUID" + hi + " or " + lo);
    }
    return equals ? "(" + columnName + " BETWEEN '" + lo + "' AND '" + hi + "')"
        : "(" + columnName + " NOT BETWEEN '" + lo + "' AND '" + hi + "')";*/
  }

  private <E extends Comparable<? super E>> Predicate toPredicate(
      Path<E> field, E value, String comparator) throws QueryValidationException {

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

  private String lookupModifier(Index schemaIndex, String modifierName) {
    if (schemaIndex != null) {
      List<String> schemaModifiers = schemaIndex.getArrayModifiers();
      if (schemaModifiers != null) {
        for (String schemaModifier : schemaModifiers) {
          if (schemaModifier.equalsIgnoreCase(modifierName)) {
            return schemaModifier;
          }
        }
      }
      String subfield = schemaIndex.getArraySubfield();
      if (subfield != null && subfield.equalsIgnoreCase(modifierName)) {
        return subfield;
      }
    }
    return null;
  }

  private Predicate indexNode(Path field, CQLTermNode node, CqlModifiers modifiers)
      throws QueryValidationException {

    // primary key
    if ("id".equals(field.getAlias()) || field.equals(root.getModel().getId(Object.class))) {
      return processId(node, field);
    }

    boolean isString = String.class.equals(field.getJavaType());

    /* if (dbIndex.isForeignKey()) {
      return pgId(node, field);
    }*/

    String comparator = node.getRelation().getBase().toLowerCase();

    switch (comparator) {
      case "=":
        if (CqlTermFormat.NUMBER.equals(modifiers.getCqlTermFormat())) {
          return queryBySql(field, node, comparator, modifiers);
        } /*else {
            return queryByFt(node, comparator, modifiers);
          }*/
      case "adj":
      case "all":
      case "any":
        // return queryByFt(field, node, comparator, modifiers);
        if (isString) {
          return queryByLike(field, node, comparator, modifiers);
        } else {
          return queryBySql(field, node, comparator, modifiers);
        }
      case "==":
      case NOT_EQUALS_OPERATOR:
        if (isString) {
          return queryByLike(field, node, comparator, modifiers);
        } else {
          return queryBySql(field, node, comparator, modifiers);
        }
      case "<":
      case ">":
      case "<=":
      case ">=":
        return queryBySql(field, node, comparator, modifiers);
      default:
        throw new CQLFeatureUnsupportedException(
            "Relation " + comparator + " not implemented yet: " + node.toString());
    }
  }

  /** Create an SQL expression using LIKE query syntax. */
  private Predicate queryByLike(
      Path field, CQLTermNode node, String comparator, CqlModifiers modifiers) {

    if (NOT_EQUALS_OPERATOR.equals(comparator)) {
      return builder.notLike(field, Cql2SqlUtil.cql2like(node.getTerm()));
    } else {
      return builder.like(field, Cql2SqlUtil.cql2like(node.getTerm()));
    }
  }

  /** Create an SQL expression using SQL as is syntax. */
  private Predicate queryBySql(
      Path field, CQLTermNode node, String comparator, CqlModifiers modifiers)
      throws QueryValidationException {

    // String term = "'" + Cql2SqlUtil.cql2like(node.getTerm()) + "'";
    // TODO: maybe handle: if (CqlTermFormat.NUMBER.equals(modifiers.getCqlTermFormat())) {
    // sql = "(" + indexMod + ")::numeric " + comparator + term;

    Comparable val = node.getTerm();

    if (Number.class.equals(field.getJavaType())) {
      val = Integer.parseInt((String) val);
    } else if (UUID.class.equals(field.getJavaType())) {
      val = UUID.fromString((String) val);
    } else if (Boolean.class.equals(field.getJavaType())) {
      val = Boolean.valueOf((String) val);
    }

    return toPredicate(field, val, comparator);
  }

  private static String wrapForLength(String term) {
    return "left(" + term + ",600)";
  }
}
