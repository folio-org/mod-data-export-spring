package org.folio.des.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final FolioModuleMetadata folioModuleMetadata;
  @Value("${folio.tenant.name}")
  private String tenant;

  public void init(Map<String, Collection<String>> okapiHeaders) {
    if (okapiHeaders == null) {
      okapiHeaders = new HashMap<>(1);
    }
    if (!okapiHeaders.containsKey(XOkapiHeaders.TENANT)) {
      okapiHeaders.put(XOkapiHeaders.TENANT, List.of(tenant));
    }
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
        new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders));
  }

  public String getHeader(FolioExecutionContext context, String headerName) {
    Map<String, Collection<String>> headers = context.getAllHeaders();
    Collection<String> headerColl = headers == null ? null : headers.get(headerName);
    return headerColl == null ? null : headerColl.stream().findFirst().filter(StringUtils::isNotBlank).orElse(null);
  }

  public String getUserName() {
    SecurityContext context = SecurityContextHolder.getContext();
    Authentication authentication = context == null ? null : context.getAuthentication();
    return authentication != null && !(authentication instanceof AnonymousAuthenticationToken) ? authentication.getName() : null;

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
