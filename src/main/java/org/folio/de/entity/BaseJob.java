package org.folio.de.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.IdentifierType;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.dto.Progress;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

@MappedSuperclass
@Data
public abstract class BaseJob {

  @Id
  @JobId
  @Column(updatable = false, nullable = false)
  private UUID id;

  private String name;

  private String description;

  private String source;

  private Boolean isSystemSource;

  @Enumerated(EnumType.STRING)
  private ExportType type;

  @Enumerated(EnumType.STRING)
  private JobStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> files = null;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<String> fileNames = null;

  private Date startTime;

  private Date endTime;

  private Date createdDate;

  private UUID createdByUserId;

  private String createdByUsername;

  private Date updatedDate;

  private UUID updatedByUserId;

  private String updatedByUsername;

  private String outputFormat;

  private String errorDetails;

  @Enumerated(EnumType.STRING)
  private BatchStatus batchStatus;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private ExitStatus exitStatus;

  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;

  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Progress progress;
}
