package org.folio.des.service;

import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;

import java.util.UUID;

public interface JobService {

  Job get(UUID id);

  JobCollection get(Integer offset, Integer limit, String query);

  Job upsert(Job job);

  void delete(UUID id);

  void deleteOldJobs();

}
