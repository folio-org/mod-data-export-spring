package org.folio.des.security;

import com.google.common.io.Resources;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.PermissionsClient;
import org.folio.des.client.UsersClient;
import org.folio.des.domain.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

  public void prepareSystemUser(String okapiUrl, String tenantId) {
    SystemUserParameters systemUserParameters = SystemUserParameters.builder()
        .id(UUID.randomUUID())
        .username(username)
        .password(username)
        .okapiUrl(okapiUrl)
        .tenantId(tenantId)
        .build();

    var folioUser = getFolioUser(username);

    if (folioUser.isPresent()) {
      updateUser(folioUser.get());
      addPermissions(folioUser.get().getId());
    } else {
      var userId = createFolioUser(username);
      authService.saveCredentials(systemUserParameters);
      assignPermissions(userId);
    }

  }

  private Optional<User> getFolioUser(String username) {
    var query = "username==" + username;
    var results = usersClient.getUsersByQuery(query);
    return results.getUsers().stream().findFirst();
  }

  private String createFolioUser(String username) {
    final var user = createUserObject(username);
    final var id = user.getId();
    usersClient.saveUser(user);
    return id;
  }

  private void updateUser(User existingUser) {
    log.info("Have to update user {}.", existingUser.getUsername());
    if (existingUserUpToDate(existingUser)) {
      log.info("User {} is up to date.", existingUser.getUsername());
    }
    usersClient.updateUser(existingUser.getId(), populateMissingUserProperties(existingUser));
    log.info("Updated user {}.", existingUser.getId());
  }

  private void assignPermissions(String userId) {
    List<String> perms = readPermissionsFromResource(PERMISSIONS_FILE_PATH);

    if (CollectionUtils.isEmpty(perms)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    var permissions = Permissions.of(UUID.randomUUID().toString(), userId, perms);

    permissionsClient.assignPermissionsToUser(permissions);
  }

  private void addPermissions(String userId) {
    var permissions = readPermissionsFromResource(PERMISSIONS_FILE_PATH);

    if (CollectionUtils.isEmpty(permissions)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    permissions.forEach(permission -> {
      var p = new Permission();
      p.setPermissionName(permission);
      try {
        permissionsClient.addPermission(userId, p);
      } catch (Exception e) {
        log.error("Error adding permission {} to {}. Permission may be already assigned.", permission, username);
      }
    });
  }

  private List<String> readPermissionsFromResource(String permissionsFilePath) {
    List<String> permissions = new ArrayList<>();
    var url = Resources.getResource(permissionsFilePath);

    try {
      permissions = Resources.readLines(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Error reading permissions from {}.", permissionsFilePath);
    }

    return permissions;
  }

  private User createUserObject(String username) {
    final var user = new User();

    user.setId(UUID.randomUUID().toString());
    user.setActive(true);
    user.setUsername(username);

    user.setPersonal(new Personal());
    user.getPersonal().setLastName(USER_LAST_NAME);

    return user;
  }

  private boolean existingUserUpToDate(User existingUser) {
    return existingUser.getPersonal() != null && StringUtils.isNotBlank(existingUser.getPersonal().getLastName());
  }

  private User populateMissingUserProperties(User existingUser) {
    existingUser.setPersonal(new Personal());
    existingUser.getPersonal().setLastName(USER_LAST_NAME);

    return existingUser;
  }

}
