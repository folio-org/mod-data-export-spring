package org.folio.des.domain.entity;

import lombok.Data;
import org.folio.des.domain.dto.JobParameterDto;
import org.folio.des.domain.entity.enums.JobType;

import java.util.Map;
import java.util.UUID;

@Data
public class Job {

  private UUID id;
  private String name;
  private String description;
  private JobType jobType;
  private Map<String, JobParameterDto> jobInputParameters;
  private JobExecution jobExecution;

}
