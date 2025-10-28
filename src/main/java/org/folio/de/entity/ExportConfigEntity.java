package org.folio.de.entity;

import java.util.List;
import java.util.UUID;

import org.folio.de.entity.base.AuditableEntity;
import org.folio.des.domain.dto.ExportType;
import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "export_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExportConfigEntity extends AuditableEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "config_name", nullable = false)
  private String configName;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private ExportType type;

  @Column(name = "tenant", nullable = false)
  private String tenant;

  @Type(JsonBinaryType.class)
  @Column(name = "export_type_specific_parameters", columnDefinition = "jsonb", nullable = false)
  private Object exportTypeSpecificParameters;

  @Column(name = "schedule_frequency")
  private Integer scheduleFrequency;

  @Column(name = "schedule_period")
  private String schedulePeriod;

  @Column(name = "schedule_time")
  private String scheduleTime;

  @Type(JsonBinaryType.class)
  @Column(name = "week_days", columnDefinition = "jsonb")
  private List<String> weekDays;

}
