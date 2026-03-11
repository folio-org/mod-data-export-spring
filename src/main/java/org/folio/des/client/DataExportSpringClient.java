package org.folio.des.client;

import org.folio.des.domain.dto.Job;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "data-export-spring")
public interface DataExportSpringClient {
  @PostExchange(value = "/jobs")
  Job upsertJob(@RequestBody Job job);

  @PostExchange(value = "/jobs/send")
  void sendJob(@RequestBody Job job);
}
