package org.folio.des.client;

import org.folio.des.domain.dto.Permission;
import org.folio.des.domain.dto.Permissions;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("perms/users")
public interface PermissionsClient {

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  Permissions assignPermissionsToUser(@RequestBody Permissions permissions);

  @PostMapping(value = "/{userId}/permissions?indexField=userId", consumes = MediaType.APPLICATION_JSON_VALUE)
  void addPermission(@PathVariable("userId") String userId, Permission permission);
}
