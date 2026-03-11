package org.folio.de.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
public class Job extends BaseJob {

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private ExportTypeSpecificParameters exportTypeSpecificParameters;
}
