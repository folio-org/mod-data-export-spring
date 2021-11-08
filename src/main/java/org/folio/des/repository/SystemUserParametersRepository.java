package org.folio.des.repository;


import org.folio.des.domain.dto.SystemUserParameters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemUserParametersRepository extends JpaRepository<SystemUserParameters, UUID> {
  Optional<SystemUserParameters> getFirstByTenantId(String tenantId);
}
