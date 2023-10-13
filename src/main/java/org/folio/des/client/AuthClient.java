package org.folio.des.client;

import org.folio.des.domain.dto.SystemUserParameters;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("authn")
public interface AuthClient {

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> getApiKey(@RequestBody SystemUserParameters authObject);

  @PostMapping(value = "/credentials", consumes = MediaType.APPLICATION_JSON_VALUE)
  void saveCredentials(@RequestBody SystemUserParameters systemUserParameters);

  @DeleteMapping(value = "/credentials", consumes = MediaType.APPLICATION_JSON_VALUE)
  void deleteCredentials(@RequestParam("userId") String userId);
}
