package org.folio.des.client;

import org.folio.des.domain.dto.AuthCredentials;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient("authn")
public interface AuthClient {

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> getApiKey(
      @RequestHeader(XOkapiHeaders.TENANT) String tenant,
      @RequestHeader(XOkapiHeaders.URL) String okapiUrl,
      @RequestBody AuthCredentials authObject);
}
