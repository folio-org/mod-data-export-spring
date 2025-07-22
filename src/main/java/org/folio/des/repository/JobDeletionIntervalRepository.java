package org.folio.des.repository;

import org.folio.de.entity.JobDeletionIntervalEntity;
import org.folio.des.domain.dto.ExportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDeletionIntervalRepository extends JpaRepository<JobDeletionIntervalEntity, ExportType> {
}
