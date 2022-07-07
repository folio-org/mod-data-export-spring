package org.folio.des.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.security.AuthService;
import org.folio.des.security.JWTokenUtils;
import org.folio.des.security.SecurityManagerService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final AuthService authService;
  private final SecurityManagerService securityManagerService;
  private boolean registered = false;

  private final Map<String, Map<String, Collection<String>>> okapiHeaders = new ConcurrentHashMap<>();

  public void storeOkapiHeaders() {
    if (MapUtils.isNotEmpty(folioExecutionContext.getOkapiHeaders())) {
      log.info("Got OKAPI headers.");
      okapiHeaders.put(folioExecutionContext.getTenantId(), folioExecutionContext.getOkapiHeaders());
    }
  }

  public void registerTenant() {
    storeOkapiHeaders();
    securityManagerService.prepareSystemUser(folioExecutionContext.getOkapiUrl(), folioExecutionContext.getTenantId());
    registered = true;
  }

  public boolean isModuleRegistered() {
    return registered;
  }

  public void initScope(String tenantId) {
    if (okapiHeaders.containsKey(tenantId) && MapUtils.isNotEmpty(okapiHeaders.get(tenantId))) {
      String url = getHeader(tenantId, XOkapiHeaders.URL);
      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
        new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders.get(tenantId)));
      if (okapiHeaders.get(tenantId).containsKey(XOkapiHeaders.TOKEN)) {
        var systemUserParameters = authService.loginSystemUser(tenantId, url);
        if (StringUtils.isNotBlank(systemUserParameters.getOkapiToken())) {
          okapiHeaders.get(tenantId).put(XOkapiHeaders.TOKEN, List.of(systemUserParameters.getOkapiToken()));
          okapiHeaders.get(tenantId).put(XOkapiHeaders.USER_ID, List.of(systemUserParameters.getUserId()));
        } else {
          throw new IllegalStateException("Can't log in and initialize FOLIO context because of absent OKAPI headers");
        }
      }
      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
        new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders.get(tenantId)));

      log.info("FOLIO context initialized with tenant {}, user {}.", folioExecutionContext.getTenantId(),
        folioExecutionContext.getUserId());
    } else {
      log.info("FOLIO context initialized with tenant {}, no logged user.", folioExecutionContext.getTenantId());
    }
  }

  public void finishContext() {
    FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
  }

  private String getHeader(String tenantId, String headerName) {
    Collection<String> headerColl = !okapiHeaders.containsKey(tenantId) ? null : okapiHeaders.get(tenantId).get(headerName);
    return headerColl == null ? null : headerColl.stream().findFirst().filter(StringUtils::isNotBlank).orElse(null);
  }

  public static String getUserName(FolioExecutionContext context) {
    String jwt = context.getToken();
    Optional<JWTokenUtils.UserInfo> userInfo = StringUtils.isBlank(jwt) ? Optional.empty() : JWTokenUtils.parseToken(jwt);
    return StringUtils.substring(userInfo.map(JWTokenUtils.UserInfo::getUserName).orElse(null), 0, 50);
  }

  public static UUID getUserId(FolioExecutionContext context) {
    String userIdStr = context.getUserId().toString();
    UUID result = null;
    if (StringUtils.isNotBlank(userIdStr)) {
      try {
        result = UUID.fromString(userIdStr);
      } catch (Exception ignore) {
        // Nothing to do
      }
    }
    return result;
  }
}
