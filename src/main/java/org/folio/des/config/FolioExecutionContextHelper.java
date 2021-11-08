package org.folio.des.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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
  private final SecurityManagerService securityManagerService;
  private boolean registered = false;
  private String tenantId;

  private final Map<String, Collection<String>> okapiHeaders = new ConcurrentHashMap<>();

  public void storeOkapiHeaders() {
    if (MapUtils.isNotEmpty(folioExecutionContext.getOkapiHeaders())) {
      log.info("Got OKAPI headers.");
      okapiHeaders.putAll(folioExecutionContext.getOkapiHeaders());
    }
  }

  public void registerTenant() {
    storeOkapiHeaders();
    securityManagerService.prepareOrUpdateSystemUser(folioExecutionContext.getOkapiUrl(), folioExecutionContext.getTenantId());
    registered = true;
    tenantId = folioExecutionContext.getTenantId();
  }

  public boolean isModuleRegistered() {
    return registered;
  }

  public void initScope() {
    if (MapUtils.isNotEmpty(okapiHeaders)) {
      String tenant = getHeader(okapiHeaders, XOkapiHeaders.TENANT);

      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
          new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders));

      var systemUserParameters = securityManagerService.getSystemUserParameters(tenant);
      if (StringUtils.isNotBlank(systemUserParameters.getOkapiToken())) {
        okapiHeaders.put(XOkapiHeaders.TOKEN, List.of(systemUserParameters.getOkapiToken()));
        FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
        FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
            new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders));

        log.info("FOLIO context initialized with tenant {}, user {}.", folioExecutionContext.getTenantId(),
            folioExecutionContext.getUserId());
      } else {
        log.info("FOLIO context initialized with tenant {}, no logged user.", folioExecutionContext.getTenantId());
      }
    } else {
      throw new IllegalStateException("Can't log in and initialize FOLIO context because of absent OKAPI headers");
    }
  }

  public static String getHeader(Map<String, Collection<String>> headers, String headerName) {
    Collection<String> headerColl = headers == null ? null : headers.get(headerName);
    return headerColl == null ? null : headerColl.stream().findFirst().filter(StringUtils::isNotBlank).orElse(null);
  }

  public static String getHeader(FolioExecutionContext context, String headerName) {
    return getHeader(context.getAllHeaders(), headerName);
  }

  public static String getUserName(FolioExecutionContext context) {
    String jwt = getHeader(context, XOkapiHeaders.TOKEN);
    Optional<JWTokenUtils.UserInfo> userInfo = StringUtils.isBlank(jwt) ? Optional.empty() : JWTokenUtils.parseToken(jwt);
    return StringUtils.substring(userInfo.map(JWTokenUtils.UserInfo::getUserName).orElse(null), 0, 50);
  }

  public static UUID getUserId(FolioExecutionContext context) {
    String userIdStr = getHeader(context, XOkapiHeaders.USER_ID);
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

  public String getTenantId() {
    return tenantId;
  }

}
