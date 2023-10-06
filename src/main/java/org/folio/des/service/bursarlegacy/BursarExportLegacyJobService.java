package org.folio.des.service.bursarlegacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.bursarlegacy.LegacyJob;
import org.folio.des.domain.dto.LegacyJobCollection;
import org.folio.des.repository.CQLService;
import org.folio.des.repository.bursarlegacy.BursarExportLegacyJobRepository;
import org.folio.des.service.util.JobMapperUtil;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class BursarExportLegacyJobService {

  private final BursarExportLegacyJobRepository repository;

  private final CQLService cqlService;

  @Transactional(readOnly = true)
  public LegacyJobCollection get(Integer offset, Integer limit, String query) {
    var result = new LegacyJobCollection();
    if (StringUtils.isBlank(query)) {
      Page<LegacyJob> page = repository.findAll(
        new OffsetRequest(offset, limit)
      );
      result.setJobRecords(page.map(this::entityToDto).getContent());
      result.setTotalRecords((int) page.getTotalElements());
    } else {
      result.setJobRecords(
        cqlService
          .getByCQL(LegacyJob.class, query, offset, limit)
          .stream()
          .map(this::entityToDto)
          .toList()
      );
      result.setTotalRecords(cqlService.countByCQL(LegacyJob.class, query));
    }
    return result;
  }

  private org.folio.des.domain.dto.LegacyJob entityToDto(LegacyJob entity) {
    return JobMapperUtil.entityToDto(entity);
  }
}
