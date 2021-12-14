package org.folio.de.entity;

import lombok.Data;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.IdentifierType;
import org.folio.des.domain.dto.Progress;
import org.springframework.batch.core.JobParameters;

import java.util.UUID;

@Data
public class JobCommand {

  public enum Type {START, DELETE}

  private Type type;
  private UUID id;
  private String name;
  private String description;
  private ExportType exportType;
  private JobParameters jobParameters;
  private IdentifierType identifierType;
  private EntityType entityType;
  private Progress progress;

}
