package org.folio.des.domain.dto.permissions;

import lombok.Data;

import java.util.List;

@Data
public class PermissionUserCollection {

  private List<PermissionUser> permissionUsers;
  private Integer totalRecords;

}
