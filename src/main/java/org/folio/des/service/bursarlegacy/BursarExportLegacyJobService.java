package org.folio.des.service.bursarlegacy;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.bursarlegacy.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.JobWithLegacyBursarParametersCollection;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.bursarlegacy.BursarExportLegacyJobRepository;
import org.folio.des.service.util.JobMapperUtil;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BursarExportLegacyJobService {

  private final BursarExportLegacyJobRepository repository;

  private final CQLService cqlService;

  @Transactional(readOnly = true)
  public JobWithLegacyBursarParametersCollection get(Integer offset, Integer limit, String query) {
    var result = new JobWithLegacyBursarParametersCollection();
    if (StringUtils.isBlank(query)) {
      Page<JobWithLegacyBursarParameters> page = repository.findAll(
        new OffsetRequest(offset, limit)
      );
      result.setJobRecords(page.map(this::entityToDto).getContent());
      result.setTotalRecords((int) page.getTotalElements());
    } else {
      result.setJobRecords(
        cqlService
          .getByCQL(JobWithLegacyBursarParameters.class, query, offset, limit)
          .stream()
          .map(this::entityToDto)
          .toList()
      );
      result.setTotalRecords(cqlService.countByCQL(JobWithLegacyBursarParameters.class, query));
    }
    return result;
  }

  private org.folio.des.domain.dto.JobWithLegacyBursarParameters entityToDto(JobWithLegacyBursarParameters entity) {
    return JobMapperUtil.entityToDto(entity);
  }
}
