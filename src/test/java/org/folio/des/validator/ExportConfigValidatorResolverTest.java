package org.folio.des.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.config.scheduling.QuartzSchemaInitializer;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Validator;

@SpringBootTest(classes = {JacksonConfiguration.class, ServiceConfiguration.class})
@EnableAutoConfiguration
class ExportConfigValidatorResolverTest {

  @Autowired
  private ExportConfigValidatorResolver resolver;
  @MockitoBean
  private Scheduler scheduler;
  @MockitoBean
  private QuartzSchemaInitializer quartzSchemaInitializer;

  @Test
  @DisplayName("Should retrieve validator for specific configuration parameter if validator is registered in the resolver")
  void shouldRetrieveValidatorForSpecificConfigurationParameters() {
    Optional<Validator> validator = resolver.resolve(ExportType.BURSAR_FEES_FINES, ExportTypeSpecificParameters.class);
    assertTrue(validator.isPresent());
  }

  @Test
  @DisplayName("Should not retrieve validator if validator is not registered in the resolver")
  void shouldNotRetrieveValidatorIfValidatorIsNotRegisteredInTheResolver() {
    Optional<Validator> validator = resolver.resolve(ExportType.CIRCULATION_LOG, ExportTypeSpecificParameters.class);
    assertFalse(validator.isPresent());
  }

  @Test
  @DisplayName("Should not fail if provided export type is not provided")
  void shouldNotFailIfProvidedExportTypeIsNotProvided() {
    Optional<Validator> validator = resolver.resolve(null, ExportTypeSpecificParameters.class);
    assertFalse(validator.isPresent());
  }
}
