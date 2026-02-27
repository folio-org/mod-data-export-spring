package org.folio.de.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.hibernate.annotations.Type;

@Entity
@Data
public class Job extends BaseJob {

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private ExportTypeSpecificParameters exportTypeSpecificParameters;
}
