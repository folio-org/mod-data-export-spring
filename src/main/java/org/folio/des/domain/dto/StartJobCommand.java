package org.folio.des.domain.dto;

import lombok.Data;
import org.springframework.batch.core.JobParameters;

import java.util.UUID;

@Data
public class StartJobCommand {

  private UUID id;
  private String name;
  private String description;
  private ExportType type;
  private JobParameters jobParameters;

}
