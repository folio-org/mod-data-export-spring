package org.folio.de.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.dto.IdentifierType;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.Progress;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Data
public class Job {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.folio.des.repository.generator.CustomUUIDGenerator")
  @Column(updatable = false, nullable = false)
  private UUID id;

  private String name;

  private String description;

  private String source;

  private Boolean isSystemSource;

  @Enumerated(EnumType.STRING)
  private ExportType type;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ExportTypeSpecificParameters exportTypeSpecificParameters;

  @Enumerated(EnumType.STRING)
  private JobStatus status;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<String> files = null;

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

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ExitStatus exitStatus;

  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;

  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Progress progress;

}
