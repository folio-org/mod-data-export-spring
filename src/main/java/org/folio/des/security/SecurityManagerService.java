package org.folio.des.security;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.PermissionsClient;
import org.folio.des.client.UsersClient;
import org.folio.des.domain.dto.Personal;
import org.folio.des.domain.dto.SystemUserParameters;
import org.folio.des.domain.dto.User;
import org.folio.des.domain.dto.permissions.Permission;
import org.folio.des.domain.dto.permissions.PermissionUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class SecurityManagerService {

  private static final String PERMISSIONS_FILE_PATH = "permissions/system-user-permissions.csv";
  private static final String USER_LAST_NAME = "SystemDataExportS";

  private final PermissionsClient permissionsClient;
  private final UsersClient usersClient;
  private final AuthService authService;

  @Value("${folio.system.username}")
  private String username;
  @Value("${folio.system.password}")
  private String password;

  public void prepareSystemUser(String okapiUrl, String tenantId) {
    Optional<User> userOptional = getUser(username);

    User user;
    if (userOptional.isPresent()) {
      user = userOptional.get();
      updateUser(user);
    } else {
      user = createUser(username);
    }

    authService.setCredentials(SystemUserParameters.builder()
        .id(UUID.randomUUID())
        .username(username)
        .password(password)
        .okapiUrl(okapiUrl)
        .tenantId(tenantId)
        .build());

    Optional<PermissionUser> permissionUserOptional = permissionsClient.get("userId==" + user.getId())
        .getPermissionUsers()
        .stream()
        .findFirst();
    if (permissionUserOptional.isPresent()) {
      addPermissions(permissionUserOptional.get());
    } else {
      createPermissionUser(user.getId());
    }
  }

  private Optional<User> getUser(String username) {
    return usersClient.getUsersByQuery("username==" + username).getUsers().stream().findFirst();
  }

  private User createUser(String username) {
    var result = createUserObject(username);
    log.info("Creating {}.", result);
    usersClient.saveUser(result);
    return result;
  }

  private void updateUser(User user) {
    if (existingUserUpToDate(user)) {
      log.info("{} is up to date.", user);
    } else {
      populateMissingUserProperties(user);
      log.info("Updating {}.", user);
      usersClient.updateUser(user.getId(), user);
    }
  }

  private PermissionUser createPermissionUser(String userId) {
    List<String> perms = readPermissionsFromResource(PERMISSIONS_FILE_PATH);
    if (CollectionUtils.isEmpty(perms)) {
      throw new IllegalStateException("No user permissions found in " + PERMISSIONS_FILE_PATH);
    }

    var permissionUser = PermissionUser.of(UUID.randomUUID().toString(), userId, perms);
    log.info("Creating {}.", permissionUser);
    return permissionsClient.create(permissionUser);
  }

  private void addPermissions(PermissionUser permissionUser) {
    var permissions = readPermissionsFromResource(PERMISSIONS_FILE_PATH);
    if (CollectionUtils.isEmpty(permissions)) {
      throw new IllegalStateException("No user permissions found in " + PERMISSIONS_FILE_PATH);
    }

    permissions.removeAll(permissionUser.getPermissions());
    permissions.forEach(permission -> {
      var p = new Permission();
      p.setPermissionName(permission);
      try {
        log.info("Adding to user {} permission {}.", permissionUser.getUserId(), p);
        permissionsClient.addPermission(permissionUser.getUserId(), p);
      } catch (Exception e) {
        log.error(String.format("Error adding permission %s to %s.", permission, username), e);
      }
    });
  }

  private List<String> readPermissionsFromResource(String permissionsFilePath) {
    List<String> result = new ArrayList<>();
    var url = Resources.getResource(permissionsFilePath);

    try {
      result = Resources.readLines(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error(String.format("Can't read user permissions from %s.", permissionsFilePath), e);
    }

    return result;
  }

  private User createUserObject(String username) {
    final var result = new User();

    result.setId(UUID.randomUUID().toString());
    result.setActive(true);
    result.setUsername(username);

    populateMissingUserProperties(result);

    return result;
  }

  private boolean existingUserUpToDate(User user) {
    return user.getPersonal() != null && StringUtils.isNotBlank(user.getPersonal().getLastName());
  }

  private User populateMissingUserProperties(User user) {
    user.setPersonal(new Personal());
    user.getPersonal().setLastName(USER_LAST_NAME);
    return user;
  }

}
