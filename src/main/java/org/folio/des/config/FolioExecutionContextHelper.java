package org.folio.des.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.security.JWTokenUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final FolioModuleMetadata folioModuleMetadata;

  public void init(Map<String, Collection<String>> okapiHeaders) {
    if (okapiHeaders == null) {
      okapiHeaders = new HashMap<>(1);
    }
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
        new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders));
  }

  public String getHeader(FolioExecutionContext context, String headerName) {
    Map<String, Collection<String>> headers = context.getAllHeaders();
    Collection<String> headerColl = headers == null ? null : headers.get(headerName);
    return headerColl == null ? null : headerColl.stream().findFirst().filter(StringUtils::isNotBlank).orElse(null);
  }

  public String getUserName(FolioExecutionContext context) {
    String jwt = getHeader(context, XOkapiHeaders.TOKEN);
    Optional<JWTokenUtils.UserInfo> userInfo = StringUtils.isBlank(jwt) ? Optional.empty() : JWTokenUtils.parseToken(jwt);
    return userInfo.map(JWTokenUtils.UserInfo::getUserName).orElse(null);
  }

  public UUID getUserId(FolioExecutionContext context) {
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
