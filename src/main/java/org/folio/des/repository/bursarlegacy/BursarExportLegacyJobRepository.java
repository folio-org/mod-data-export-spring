package org.folio.des.repository.bursarlegacy;

import java.util.UUID;
import org.folio.de.entity.bursarlegacy.JobWithLegacyBursarParameters;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BursarExportLegacyJobRepository extends JpaRepository<JobWithLegacyBursarParameters, UUID> {
}
