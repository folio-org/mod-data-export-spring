package org.folio.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import tools.jackson.databind.JsonNode;

@HttpExchange("configurations")
public interface ConfigurationClient {

  @GetExchange("/entries")
  JsonNode getConfigurations(@RequestParam("query") String query, @RequestParam("limit") int limit);

}
