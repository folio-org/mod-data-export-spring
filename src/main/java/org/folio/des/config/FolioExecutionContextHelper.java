package org.folio.des.config;

import static java.util.Objects.nonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final AuthService authService;
  private final SecurityManagerService securityManagerService;
  private boolean registered = false;
  // TODO find a way to get current 'x-okapi-url', might be list of urls
  private final String okapiUrl = "http://10.0.2.15:9130";

  public void registerTenant() {
    securityManagerService.prepareSystemUser(folioExecutionContext.getOkapiUrl(), folioExecutionContext.getTenantId());
    registered = true;
  }

  public boolean isModuleRegistered() {
    return registered;
  }

  public FolioExecutionContext getFolioExecutionContext(String tenantId) {
    Map<String, Collection<String>> tenantOkapiHeaders = new ConcurrentHashMap<>();
    tenantOkapiHeaders.put(XOkapiHeaders.TENANT, List.of(tenantId));
    tenantOkapiHeaders.put(XOkapiHeaders.URL, List.of(okapiUrl));

    String token = authService.getTokenForSystemUser(tenantId, okapiUrl);
    if (StringUtils.isNotBlank(token)) {
      tenantOkapiHeaders.put(XOkapiHeaders.TOKEN, List.of(token));

      try (var context = new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders))) {
        String userId = authService.getSystemUserId();
        if (nonNull(userId)) {
          tenantOkapiHeaders.put(XOkapiHeaders.USER_ID, List.of(userId));
        }
      }
      return new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders);
    } else {
      throw new IllegalStateException(String.format("Cannot create FolioExecutionContext for Tenant: %s because of absent token", tenantId));
    }
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
