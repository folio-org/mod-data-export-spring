package org.folio.des.client;

import org.folio.des.config.feign.FeignClientConfiguration;
import org.folio.des.domain.dto.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "data-export-spring", configuration = FeignClientConfiguration.class)
public interface DataExportSpringClient {
  @PostMapping(value = "/jobs")
  Job upsertJob(@RequestBody Job job);

  @PostMapping(value = "/jobs/send")
  void sendJob(@RequestBody Job job);
}
