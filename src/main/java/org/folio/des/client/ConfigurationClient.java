package org.folio.des.client;

import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ModelConfiguration;
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
  ConfigurationCollection getConfigurations(@RequestParam("query") String query);

  @PostMapping
  ModelConfiguration postConfiguration(@RequestBody ModelConfiguration config);

  @PutMapping(path = "/{entryId}")
  void putConfiguration(@RequestBody ModelConfiguration config, @PathVariable String entryId);

  @GetMapping(path = "/{entryId}")
  ModelConfiguration getConfigById(@PathVariable String entryId);

}
