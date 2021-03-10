package org.folio.des.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.AuthClient;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.AuthCredentials;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthService {

  private final AuthClient authClient;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioExecutionContextHelper executionContextHelper;

  @Value("${folio.username}")
  private String username;
  @Value("${folio.password}")
  private String password;

  private Map<String, Collection<String>> okapiHeaders;

  public void storeOkapiHeaders() {
    log.info("Got OKAPI headers.");
    okapiHeaders = folioExecutionContext.getOkapiHeaders();
  }

  public void initializeFolioScope() {
    if (okapiHeadersExist()) {
      login();
      executionContextHelper.init(okapiHeaders);
    } else {
      throw new IllegalStateException("Can't log in and initialize FOLIO context because of absent OKAPI headers");
    }
  }

  public boolean okapiHeadersExist() {
    return okapiHeaders != null && StringUtils.isNotBlank(
        executionContextHelper.getHeader(okapiHeaders, XOkapiHeaders.TENANT)) && StringUtils.isNotBlank(
        executionContextHelper.getHeader(okapiHeaders, XOkapiHeaders.URL));
  }

  private void login() {
    AuthCredentials credentials = new AuthCredentials();
    credentials.setUsername(username);
    credentials.setPassword(password);

    ResponseEntity<String> authResponse = authClient.getApiKey(executionContextHelper.getHeader(okapiHeaders, XOkapiHeaders.TENANT),
        executionContextHelper.getHeader(okapiHeaders, XOkapiHeaders.URL), credentials);
    log.info("Logged in as {}.", username);

    okapiHeaders.put(XOkapiHeaders.TOKEN, authResponse.getHeaders().get(XOkapiHeaders.TOKEN));
  }

}
