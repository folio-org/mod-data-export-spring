package org.folio.des.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.security.AuthService;
import org.folio.des.security.JWTokenUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Log4j2
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final AuthService authService;

  private final Map<String, Collection<String>> okapiHeaders = new HashMap<>();

  public void storeOkapiHeaders() {
    if (folioExecutionContext.getOkapiHeaders() != null && !folioExecutionContext.getOkapiHeaders().isEmpty()) {
      log.info("Got OKAPI headers.");
      okapiHeaders.putAll(folioExecutionContext.getOkapiHeaders());
    }
  }

  public void init() {
    String tenant = getHeader(okapiHeaders, XOkapiHeaders.TENANT);
    if (StringUtils.isNotBlank(tenant)) {
      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
          new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders));
      String url = getHeader(okapiHeaders, XOkapiHeaders.URL);
      if (StringUtils.isNotBlank(url)) {
        List<String> token = authService.login(tenant, url);
        if (CollectionUtils.isNotEmpty(token) && StringUtils.isNotBlank(token.get(0))) {
          okapiHeaders.put(XOkapiHeaders.TOKEN, token);
          FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
              new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders));
        }
      }
      log.info("FOLIO context initialized.");
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
      }
    }
    return result;
  }

}
