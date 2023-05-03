package org.folio.de.entity.bursarlegacy;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.IdentifierType;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.dto.LegacyExportTypeSpecificParameters;
import org.folio.des.domain.dto.Progress;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

@Entity
@Table(name = "job")
@Data
public class LegacyJob {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(
    name = "UUID",
    strategy = "org.folio.des.repository.generator.CustomUUIDGenerator"
  )
  @Column(updatable = false, nullable = false)
  private UUID id;

  private String name;

  private String description;

  private String source;

  private Boolean isSystemSource;

  @Enumerated(EnumType.STRING)
  private ExportType type;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private LegacyExportTypeSpecificParameters exportTypeSpecificParameters;

  @Enumerated(EnumType.STRING)
  private JobStatus status;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<String> files = null;

  @Type(JsonBinaryType.class)
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

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private ExitStatus exitStatus;

  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;

  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Progress progress;
}
