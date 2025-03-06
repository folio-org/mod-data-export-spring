package org.folio.de.entity;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.des.repository.generator.CustomUUIDGenerator;
import org.hibernate.annotations.IdGeneratorType;

@IdGeneratorType( CustomUUIDGenerator.class)
@Retention( RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD})
public @interface JobId {
}
