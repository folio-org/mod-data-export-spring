package org.folio.des.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ExportConfig entity to manage export configurations.
 */
@Repository
public interface ExportConfigRepository extends JpaCqlRepository<ExportConfigEntity, UUID> {

  @Query(value = """
    SELECT * FROM export_config
    WHERE type IN (:types)
      AND export_type_specific_parameters -> 'vendorEdiOrdersExportConfig' ->> 'vendorId' = :vendorId
    ORDER BY config_name""", nativeQuery = true)
  List<ExportConfigEntity> findByTypesAndVendorId(@Param("types") Collection<String> types, @Param("vendorId") String vendorId);

}

