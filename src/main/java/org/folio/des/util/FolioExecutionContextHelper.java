package org.folio.des.util;

import static java.util.Objects.nonNull;

import java.util.Optional;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.security.JWTokenUtils;
import org.folio.spring.FolioExecutionContext;

@UtilityClass
public class FolioExecutionContextHelper {
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
