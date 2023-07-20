package org.folio.des.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.AuthClient;
import org.folio.des.client.UsersClient;
import org.folio.des.domain.dto.SystemUserParameters;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthService {

  private final AuthClient authClient;
  private final UsersClient usersClient;

  @Value("${folio.system.username}")
  private String username;
  @Value("${folio.system.password}")
  private String password;

  public SystemUserParameters loginSystemUser(String tenant, String url) {
    SystemUserParameters userParameters =
        SystemUserParameters.builder()
            .okapiUrl(url)
            .tenantId(tenant)
            .username(username)
            .password(password)
            .build();

    log.info("Login with url={} tenant={} username={}.", url, tenant, username);

    ResponseEntity<String> authResponse = authClient.getApiKey(userParameters);

    var token = authResponse.getHeaders().get(XOkapiHeaders.TOKEN);
    if (isNotEmpty(token)) {
      userParameters.setOkapiToken(token.get(0));
      usersClient.getUsersByQuery("username==" + username).getUsers().stream().findFirst().ifPresentOrElse(user ->
        userParameters.setUserId(user.getId()), () -> log.error("Can't find user id by username {}.", username));
      log.info("Logged in as {}.", username);
    } else {
      log.error("Can't get token logging in as {}.", username);
    }
    return userParameters;
  }

  private boolean isNotEmpty(java.util.List<String> token) {
    return CollectionUtils.isNotEmpty(token) && StringUtils.isNotBlank(token.get(0));
  }

  public void deleteCredentials(String userId) {
    authClient.deleteCredentials(userId);

    log.info("Removed credentials for user {}.", userId);
  }

  public void saveCredentials(SystemUserParameters systemUserParameters) {
    authClient.saveCredentials(systemUserParameters);

    log.info("Saved credentials for user {}.", systemUserParameters.getUsername());
  }

}
