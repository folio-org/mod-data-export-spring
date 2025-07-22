package org.folio.de.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.folio.des.domain.dto.ExportType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_deletion_interval")
@Data
public class JobDeletionIntervalEntity {
  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "export_type")
  private ExportType exportType;

  @Column(name = "retention_days", nullable = false)
  private Integer retentionDays;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "updated_date")
  private LocalDateTime updatedDate;

  @Column(name = "updated_by")
  private UUID updatedBy;
}
