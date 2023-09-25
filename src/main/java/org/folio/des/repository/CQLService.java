package org.folio.des.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.des.repository.criteria.CQL2JPACriteria;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class CQLService {

  @PersistenceContext
  private EntityManager entityManager;

  public <E> List<E> getByCQL(Class<E> entityCls, String cql, int offset, int limit) {
    log.debug("getByCQL:: by cql={} with offset={} and limit={} for {}.", cql, offset, limit, entityCls);
    try {
      final CQL2JPACriteria<E> cql2JPACriteria = new CQL2JPACriteria<>(entityCls, entityManager);
      final CriteriaQuery<E> criteria = cql2JPACriteria.toCriteria(cql);
      return entityManager
          .createQuery(criteria)
          .setFirstResult(offset)
          .setMaxResults(limit)
          .getResultList();
    } catch (QueryValidationException e) {
      log.error("Can not invoke CQL query {} ", cql);
      throw new IllegalArgumentException(e);
    }
  }

  public <E> Integer countByCQL(Class<E> entityCls, String cql) {
    log.debug("countByCQL:: by cql={} for {}.", cql, entityCls);
    try {
      final CQL2JPACriteria<E> cql2JPACriteria = new CQL2JPACriteria<>(entityCls, entityManager);
      final CriteriaQuery<E> criteria = cql2JPACriteria.toCriteria(cql);
      return entityManager.createQuery(criteria).getResultList().size();
    } catch (QueryValidationException e) {
      log.error("Can not invoke CQL query {} ", cql);
      throw new IllegalArgumentException(e);
    }
  }
}
