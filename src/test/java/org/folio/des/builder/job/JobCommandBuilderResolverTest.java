package org.folio.des.builder.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.domain.dto.ExportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {JacksonConfiguration.class, ServiceConfiguration.class})
class JobCommandBuilderResolverTest {

  @Autowired
  private JobCommandBuilderResolver resolver;
  @MockBean
  private ConfigurationClient client;

  @ParameterizedTest
  @DisplayName("Should retrieve builder for specific export type if builder is registered in the resolver")
  @CsvSource({
    "BURSAR_FEES_FINES, BurSarFeeFinesJobCommandBuilder",
    "CIRCULATION_LOG, CirculationLogJobCommandBuilder",
    "BULK_EDIT_QUERY, BulkEditQueryJobCommandBuilder",
    "EDIFACT_ORDERS_EXPORT, EdifactOrdersJobCommandBuilder"
  })
  void shouldRetrieveBuilderForSpecifiedExportTypeIfBuilderIsRegisteredInTheResolver(ExportType exportType,
              String expBuilderClass) {
    Optional<JobCommandBuilder> builder = resolver.resolve(exportType);
    assertEquals(expBuilderClass, builder.get().getClass().getSimpleName());
  }

  @Test
  @DisplayName("Should not retrieve builder for specific export type if builder is not registered in the resolver")
  void shouldRetrieveBuilderForSpecifiedExportTypeIfBuilderIsRegisteredInTheResolver() {
    Optional<JobCommandBuilder> builder = resolver.resolve(ExportType.EDIFACT_ORDERS_EXPORT);
    assertTrue(builder.isPresent());
  }
}
