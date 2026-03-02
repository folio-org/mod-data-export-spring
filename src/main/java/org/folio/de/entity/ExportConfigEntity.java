package org.folio.de.entity;

import java.util.List;
import java.util.UUID;

import org.folio.de.entity.base.AuditableEntity;
import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "export_config")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ExportConfigEntity extends AuditableEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "config_name", nullable = false)
  private String configName;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "tenant", nullable = false)
  private String tenant;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "export_type_specific_parameters", columnDefinition = "jsonb", nullable = false)
  private Object exportTypeSpecificParameters;

  @Column(name = "schedule_frequency")
  private Integer scheduleFrequency;

  @Column(name = "schedule_period")
  private String schedulePeriod;

  @Column(name = "schedule_time")
  private String scheduleTime;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "week_days", columnDefinition = "jsonb")
  private List<String> weekDays;

}
