package org.folio.des.repository.generator;

import java.io.Serializable;
import lombok.NoArgsConstructor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerator;

@NoArgsConstructor
public class CustomUUIDGenerator extends UUIDGenerator {

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    Serializable id =
      (Serializable) session.getEntityPersister(null, object).getIdentifier(object, session);
    return id != null ? id : (Serializable) super.generate(session, object);
  }
}
