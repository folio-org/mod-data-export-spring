package org.folio.des.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class JobDto extends BaseJobDto {

  private UUID id;
  private JobExecutionDto jobExecution;

}
