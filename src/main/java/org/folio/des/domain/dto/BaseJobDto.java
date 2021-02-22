package org.folio.des.domain.dto;

import lombok.Data;
import org.folio.des.domain.entity.enums.JobType;

@Data
public class BaseJobDto {

  private String name;
  private String description;
  private JobType jobType;

}
