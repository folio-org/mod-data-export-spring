package org.folio.des.service.bursarlegacy;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.de.entity.bursarlegacy.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.repository.bursarlegacy.BursarExportLegacyJobRepository;
import org.folio.des.service.util.JobMapperUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BursarExportLegacyJobService {

  private final BursarExportLegacyJobRepository repository;

  @Transactional(readOnly = true)
  public List<org.folio.des.domain.dto.JobWithLegacyBursarParameters> getAllLegacyJobs() {
    return repository.findAll()
      .stream()
      .filter(BursarExportLegacyJobService::hasLegacyBursarParameters)
      .map(BursarExportLegacyJobService::entityToDto)
      .toList();
  }

  public static boolean hasLegacyBursarParameters(JobWithLegacyBursarParameters job) {
    // ensure legacy `bursarFeeFines` is present and actually contains values
    LegacyBursarFeeFines legacyObject = job.getExportTypeSpecificParameters()
      .getBursarFeeFines();

    return (legacyObject != null && legacyObject.getDaysOutstanding() != null);
  }

  public static org.folio.des.domain.dto.JobWithLegacyBursarParameters entityToDto(JobWithLegacyBursarParameters entity) {
    return JobMapperUtil.entityToDto(entity);
  }
}
