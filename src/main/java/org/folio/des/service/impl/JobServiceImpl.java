package org.folio.des.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.service.JobService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobServiceImpl implements JobService {

  @Override
  public Job get(UUID id) {
    return null;
  }

  @Override
  public JobCollection get(Integer offset, Integer limit, String query) {
    return null;
  }

  @Override
  public Job upsert(Job job) {
    return null;
  }

  @Override
  public void delete(UUID id) {

  }

}
