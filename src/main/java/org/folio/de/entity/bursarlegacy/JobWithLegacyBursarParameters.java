package org.folio.de.entity.bursarlegacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.folio.de.entity.BaseJob;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job")
@Data
public class JobWithLegacyBursarParameters extends BaseJob {

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private ExportTypeSpecificParametersWithLegacyBursar exportTypeSpecificParameters;
}
