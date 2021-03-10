package org.folio.des.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.folio.des.client.AuthClient;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.AuthCredentials;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthClient authClient;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioExecutionContextHelper executionContextHelper;

  @Value("${folio.tenant.password}")
  private String password;
  @Value("${folio.tenant.username}")
  private String username;

  private Map<String, Collection<String>> okapiHeaders;

  public void authorizeModuleWithSystemUser() {
    okapiHeaders = folioExecutionContext.getOkapiHeaders();
    AuthCredentials authDto = createCredentials();

    String tenant = executionContextHelper.getHeader(folioExecutionContext, XOkapiHeaders.TENANT);
    String okapiUrl = executionContextHelper.getHeader(folioExecutionContext, XOkapiHeaders.URL);

    ResponseEntity<String> authResponse = authClient.getApiKey(tenant, okapiUrl, authDto);

    HttpHeaders headers = authResponse.getHeaders();
    List<String> token = headers.get(XOkapiHeaders.TOKEN);

    okapiHeaders.put(XOkapiHeaders.TOKEN, token);
  }

  public void initializeFolioScope() {
    if (isTenantRegistered() && folioExecutionContext.getTenantId() == null) {
      executionContextHelper.init(okapiHeaders);
    }
  }

  public boolean isTenantRegistered() {
    return okapiHeaders != null;
  }

  private AuthCredentials createCredentials() {
    AuthCredentials authDto = new AuthCredentials();
    authDto.setPassword(password);
    authDto.setUsername(username);
    return authDto;
  }

}
