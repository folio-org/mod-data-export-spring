package org.folio.des.client;

import org.folio.des.domain.dto.permissions.Permission;
import org.folio.des.domain.dto.permissions.PermissionUser;
import org.folio.des.domain.dto.permissions.PermissionUserCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient("perms/users")
public interface PermissionsClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PermissionUserCollection get(@RequestParam("query") String query);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  PermissionUser create(@RequestBody PermissionUser permissionUser);

  @PostMapping(value = "/{userId}/permissions?indexField=userId", consumes = MediaType.APPLICATION_JSON_VALUE)
  void addPermission(@PathVariable("userId") String userId, Permission permission);

}
