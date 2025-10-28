package org.folio.des.repository;

import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ExportConfig entity to manage export configurations.
 */
@Repository
public interface ExportConfigRepository extends JpaCqlRepository<ExportConfigEntity, UUID> {

}

