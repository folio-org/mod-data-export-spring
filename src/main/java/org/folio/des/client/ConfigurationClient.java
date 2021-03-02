package org.folio.des.client;

import org.folio.des.domain.dto.ConfigModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "configurations/entries")
public interface ConfigurationClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  String getConfiguration(@RequestParam("query") String query);

  @PostMapping
  ConfigModel postConfiguration(@RequestBody ConfigModel config);

  @PutMapping(path = "/{entryId}")
  void putConfiguration(@RequestBody ConfigModel config, @PathVariable String entryId);

}
