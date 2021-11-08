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
import org.folio.des.repository.SystemUserParametersRepository;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@Log4j2
@RequiredArgsConstructor
public class SecurityManagerService {

  private static final String PERMISSIONS_FILE_PATH = "permissions/system-user-permissions.csv";
  private static final String USER_LAST_NAME = "SystemDataExportS";

  private final PermissionsClient permissionsClient;
  private final UsersClient usersClient;
  private final AuthService authService;
  private final SystemUserParametersRepository systemUserParametersRepository;

  @PersistenceContext
  private final EntityManager em;

  private final FolioModuleMetadata moduleMetadata;

  @Value("${folio.system.username}")
  private String username;

  public void prepareOrUpdateSystemUser(String okapiUrl, String tenantId) {

    var systemUserParameters = buildDefaultSystemUserParameters(username, username, okapiUrl, tenantId);

    var folioUser = getFolioUser(username);

    if (folioUser.isPresent()) {
      updateUser(folioUser.get());
      addPermissions(folioUser.get().getId());
    } else {
      var createdUser = createFolioUser(username);
      authService.saveCredentials(systemUserParameters);
      createPermissionUser(createdUser.getId());
    }

    var backgroundUserApiKey = authService.loginSystemUser(systemUserParameters.getUsername(), systemUserParameters.getOkapiUrl());
    systemUserParameters.setOkapiToken(backgroundUserApiKey.getOkapiToken());
    saveOrUpdateSystemUserParameters(systemUserParameters);
  }

  /**
   * This method saves or replaces existing {@link SystemUserParameters} record if this record already exists for tenant
   * {@link SystemUserParameters#getTenantId()}
   *
   * @param systemUserParams - system user parameters to be saved or replaced (if it already exists for tenant)
   */
  private void saveOrUpdateSystemUserParameters(SystemUserParameters systemUserParams) {
    systemUserParametersRepository.getFirstByTenantId(systemUserParams.getTenantId())
      .ifPresent(existingSystemUserParameters -> systemUserParams.setId(existingSystemUserParameters.getId()));
    systemUserParametersRepository.save(systemUserParams);
  }

  private SystemUserParameters buildDefaultSystemUserParameters(String username, String password, String okapiUrl, String tenantId) {
    return SystemUserParameters.builder()
      .id(UUID.randomUUID())
      .username(username)
      .password(password)
      .okapiUrl(okapiUrl)
      .tenantId(tenantId).build();
  }

  public SystemUserParameters getSystemUserParameters(String tenantId) {
    final String sqlQuery = "SELECT * FROM " + moduleMetadata.getDBSchemaName(tenantId) + ".system_user_parameters";
    var query = em.createNativeQuery(sqlQuery, SystemUserParameters.class); //NOSONAR
    return (SystemUserParameters) query.getSingleResult();
  }

  private Optional<User> getFolioUser(String username) {
    return usersClient.getUsersByQuery("username==" + username).getUsers().stream().findFirst();
  }

  private User createFolioUser(String username) {
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

  private void addPermissions(String userId) {
    var permissions = readPermissionsFromResource(PERMISSIONS_FILE_PATH);

    if (isEmpty(permissions)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    permissions.forEach(permission -> {
      var p = new Permission();
      p.setPermissionName(permission);
      try {
        permissionsClient.addPermission(userId, p);
      } catch (Exception e) {
        log.info("Error adding permission {} to System User. Permission may be already assigned.", permission);
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
