package org.folio.des.config;

import static java.util.Objects.nonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.security.AuthService;
import org.folio.des.security.JWTokenUtils;
import org.folio.des.security.SecurityManagerService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final AuthService authService;
  private final SecurityManagerService securityManagerService;
  @Value("${folio.okapi.url}")
  private String okapiUrl;

  public void registerTenant() {
    securityManagerService.prepareSystemUser(folioExecutionContext.getOkapiUrl(), folioExecutionContext.getTenantId());
  }

  public FolioExecutionContext getFolioExecutionContext(String tenantId) {
    Map<String, Collection<String>> tenantOkapiHeaders = new HashMap<>() {{
      put(XOkapiHeaders.TENANT, List.of(tenantId));
      put(XOkapiHeaders.URL, List.of(okapiUrl));
    }};

    // We only have headers['tenant', 'url'] to set up 'execution context' with minimum headers['tenant', 'url', 'token', 'user'].
    // We will do it in two steps: Calling 'auth/login' does not require any permission, so in first one we create 'execution context' with headers['tenant', 'url']
    // and should be able to get a 'token' with required permissions. And then we start second 'execution context' with headers['tenant', 'url', 'token']
    // to get 'system-user-id', at this point we already have 'token' so request is authorized. ('system-user' is created when 'tenant' is registered)
    try (var context = new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders))) {
      String systemUserToken = authService.getTokenForSystemUser(tenantId, okapiUrl);
      if (StringUtils.isNotBlank(systemUserToken)) {
        tenantOkapiHeaders.put(XOkapiHeaders.TOKEN, List.of(systemUserToken));
      } else {
        // If we do not get a 'token' we will not have required permissions to do further requests so stop the process
        throw new IllegalStateException(String.format("Cannot create FolioExecutionContext for Tenant: %s because of absent token", tenantId));
      }
    }

    try (var context = new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders))) {
      String systemUserId = authService.getSystemUserId();
      if (nonNull(systemUserId)) {
        tenantOkapiHeaders.put(XOkapiHeaders.USER_ID, List.of(systemUserId));
      }
    }
    return new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders);
  }

  public static String getUserName(FolioExecutionContext context) {
    String jwt = context.getToken();
    Optional<JWTokenUtils.UserInfo> userInfo = StringUtils.isBlank(jwt) ? Optional.empty() : JWTokenUtils.parseToken(jwt);
    return StringUtils.substring(userInfo.map(JWTokenUtils.UserInfo::getUserName).orElse(null), 0, 50);
  }

  public static UUID getUserId(FolioExecutionContext context) {
    var userIdStr = context.getUserId();
    UUID result = null;
    if (nonNull(userIdStr)) {
      try {
        result = userIdStr;
      } catch (Exception ignore) {
        // Nothing to do
      }
    }
    return result;
  }
}
