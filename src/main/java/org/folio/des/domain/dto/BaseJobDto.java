package org.folio.des.domain.dto;

import lombok.Data;

@Data
public class BaseJobDto {

  private String name;
  private String description;
  private ExportType type;

}
