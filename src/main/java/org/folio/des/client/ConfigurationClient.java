package org.folio.des.client;

import org.folio.des.domain.dto.bursar.ConfigModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "configurations/entries")
public interface ConfigurationClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  String getConfiguration(@RequestParam("query") String query);

  @PostMapping
  ConfigModel postConfiguration(@RequestBody ConfigModel config);

  @PutMapping(path = "/{entryId}")
  void putConfiguration(@RequestBody ConfigModel config, @PathVariable String entryId);
}
