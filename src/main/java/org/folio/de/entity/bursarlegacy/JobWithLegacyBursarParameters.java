package org.folio.de.entity.bursarlegacy;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.folio.de.entity.BaseJob;
import org.folio.des.domain.dto.LegacyExportTypeSpecificParameters;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "job")
@Data
public class JobWithLegacyBursarParameters extends BaseJob {

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private LegacyExportTypeSpecificParameters exportTypeSpecificParameters;
}
