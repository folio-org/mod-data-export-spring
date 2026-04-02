package org.folio.des.service.config;

import static org.folio.des.support.TestUtils.loadData;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.client.ConfigurationClient;
import org.folio.des.CopilotGenerated;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.support.BaseTest;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=diku_mod_data_export_spring")
@CopilotGenerated(partiallyGenerated = true, model = "Claude Opus 4.6")
class ConfigurationMigrationServiceTest extends BaseTest {

  @Autowired
  private ConfigurationMigrationService migrationService;

  @Autowired
  private ExportConfigRepository exportConfigRepository;

  @MockitoBean
  private ConfigurationClient configurationClient;

  @ParameterizedTest
  @DisplayName("Should migrate when upgrading from old version or fresh install (null)")
  @CsvSource(value = {"mod-data-export-spring-3.6.0-SNAPSHOT", "mod-data-export-spring-3.6.0-SNAPSHOT.123", "3.0.0", "''", "null"}, nullValues = "null")
  void shouldMigrateWhenVersionRequiresMigration(String moduleFrom) {
    var attributes = tenantAttributes(moduleFrom);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(loadData("data/configuration-entries/empty-response.json"));

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
    verify(configurationClient).getConfigurations(anyString(), anyInt());
  }

  @ParameterizedTest
  @DisplayName("Should skip migration when version is at or above target")
  @CsvSource({"mod-data-export-spring-3.6.1-SNAPSHOT", "mod-data-export-spring-3.6.0", "mod-data-export-spring-4.0.0"})
  void shouldSkipMigrationWhenVersionDoesNotRequireMigration(String moduleFrom) {
    var attributes = tenantAttributes(moduleFrom);

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
    verify(configurationClient, never()).getConfigurations(anyString(), anyInt());
  }

  @Test
  @DisplayName("Should handle configuration client exception gracefully")
  void shouldHandleConfigurationClientException() {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenThrow(new RuntimeException("Connection refused"));

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
  }

  @Test
  @DisplayName("Should handle null response from configuration client")
  void shouldHandleNullConfigurationResponse() {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(null);

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
  }

  @Test
  @DisplayName("Should handle empty configs array from configuration client")
  void shouldHandleEmptyConfigsArray() {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(loadData("data/configuration-entries/empty-response.json"));

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
    assertEquals(0, exportConfigRepository.count());
  }

  @Test
  @DisplayName("Should insert valid configuration entry")
  void shouldInsertValidConfigurationEntry() {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(loadData("data/configuration-entries/valid-entry-response.json"));

    migrationService.migrateConfigurationData(attributes, TENANT);

    assertEquals(1, exportConfigRepository.count());
  }

  @Test
  @DisplayName("Should handle duplicate records in response by inserting only once")
  void shouldHandleDuplicateRecordsInResponse() {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(loadData("data/configuration-entries/duplicate-entries-response.json"));

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
    assertEquals(1, exportConfigRepository.count());
  }

  @Test
  @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    scripts = "classpath:data/configuration-entries/existing-export-config.sql")
  @DisplayName("Should not overwrite existing DB record when config with same id is received")
  void shouldNotOverwriteExistingDbRecord() {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(loadData("data/configuration-entries/conflicting-entry-response.json"));

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));

    // Verify the original record is unchanged
    assertEquals(1, exportConfigRepository.count());
    var entity = exportConfigRepository.findById(UUID.fromString("a1111111-1111-1111-1111-111111111111"));
    assertTrue(entity.isPresent());
    assertEquals("BURSAR_FEES_FINES", entity.get().getType());
  }

  @Test
  @DisplayName("Should skip config entries with missing type")
  void shouldSkipEntriesWithMissingType() {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(loadData("data/configuration-entries/missing-type-response.json"));

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
    assertEquals(0, exportConfigRepository.count());
  }

  @ParameterizedTest
  @DisplayName("Should skip config entries with invalid exportTypeSpecificParameters")
  @ValueSource(strings = {
    "data/configuration-entries/missing-params-response.json",
    "data/configuration-entries/invalid-params-response.json"
  })
  void shouldSkipEntriesWithInvalidExportTypeSpecificParameters(String responsePath) {
    var attributes = tenantAttributes(null);
    when(configurationClient.getConfigurations(anyString(), anyInt())).thenReturn(loadData(responsePath));

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
    assertEquals(0, exportConfigRepository.count());
  }

  @Test
  @DisplayName("Should be idempotent when migration is run twice with the same data")
  void shouldBeIdempotentWhenMigrationRunTwice() {
    var attributes = tenantAttributes(null);
    var response = loadData("data/configuration-entries/valid-entry-response.json");
    when(configurationClient.getConfigurations(anyString(), anyInt()))
      .thenReturn(response);

    migrationService.migrateConfigurationData(attributes, TENANT);
    assertEquals(1, exportConfigRepository.count());

    assertDoesNotThrow(() -> migrationService.migrateConfigurationData(attributes, TENANT));
    assertEquals(1, exportConfigRepository.count());
  }

  private TenantAttributes tenantAttributes(String moduleFrom) {
    var attrs = new TenantAttributes();
    attrs.setModuleFrom(moduleFrom);
    attrs.setModuleTo("mod-data-export-spring-3.0.0");
    return attrs;
  }
}



