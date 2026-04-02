package org.folio.des.service.config;

import static org.folio.des.scheduling.util.ScheduleUtil.moduleVersionToSemVer;

import org.apache.commons.lang3.StringUtils;
import org.folio.client.ConfigurationClient;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.spring.FolioModuleMetadata;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConfigurationMigrationService {

  private static final String MIGRATION_TARGET_VERSION = "3.6.0";
  private static final String CONFIGURATION_QUERY = "module==mod-data-export-spring";
  private static final String EXPORT_CONFIG_TABLE = "export_config";
  private static final String EXPORT_CONFIG_SQL = """
    INSERT INTO %s.%s (
      id, config_name, type, tenant, export_type_specific_parameters,
      schedule_frequency, schedule_period, schedule_time, week_days,
      created_date, created_by, updated_date, updated_by
    ) VALUES (
      ?::uuid, ?, ?, ?, ?::jsonb,
      ?, ?, ?, ?::jsonb,
      ?::timestamp, ?::uuid, ?::timestamp, ?::uuid
    ) ON CONFLICT (id) DO NOTHING
    """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final ConfigurationClient configurationClient;
  private final FolioModuleMetadata folioModuleMetadata;

  public void migrateConfigurationData(TenantAttributes attributes, String tenantId) {
    log.info("migrateConfigurationData:: Attempting to migrate configuration data from mod-configuration for tenant: {}", tenantId);
    if (!isMigrationNeeded(attributes)) {
      log.info("migrateConfigurationData:: Migration not needed for tenant: {}. Skipping configuration data migration.", tenantId);
      return;
    }

    try {
      var configsResponse = fetchConfigurationEntries();
      if (configsResponse == null || !configsResponse.has("configs") || configsResponse.get("configs").isEmpty()) {
        log.info("migrateConfigurationData:: No configuration entries found to migrate");
        return;
      }
      configsResponse.get("configs").forEach(config -> migrateConfigEntry(tenantId, config));
    } catch (Exception e) {
      log.warn("migrateConfigurationData:: Failed to migrate configuration data from mod-configuration. ", e);
    }
  }

  private static boolean isMigrationNeeded(TenantAttributes tenantAttributes) {
    var moduleFrom = tenantAttributes.getModuleFrom();
    if (StringUtils.isBlank(moduleFrom)) {
      return true;
    }
    return moduleVersionToSemVer(MIGRATION_TARGET_VERSION).compareTo(moduleVersionToSemVer(moduleFrom)) > 0;
  }

  private JsonNode fetchConfigurationEntries() {
    return configurationClient.getConfigurations(CONFIGURATION_QUERY, Integer.MAX_VALUE);
  }

  private void migrateConfigEntry(String tenantId, JsonNode config) {
    var id = config.path("id").asString();

    try {
      JsonNode valueObj = objectMapper.readTree(config.path("value").asString("{}"));

      var type = valueObj.path("type").asString(null);
      var exportTypeSpecificParameters = valueObj.path("exportTypeSpecificParameters");

      if (StringUtils.isBlank(type) || type.equals("null") || !validateExportTypeSpecificParams(exportTypeSpecificParameters)) {
        log.warn("migrateConfigEntry:: Skipping config {} due to missing type or exportTypeSpecificParameters", id);
        return;
      }

      var metadata = config.path("metadata");
      var weekDays = valueObj.path("weekDays");

      jdbcTemplate.update(EXPORT_CONFIG_SQL.formatted(folioModuleMetadata.getDBSchemaName(tenantId), EXPORT_CONFIG_TABLE),
        id,
        config.path("configName").asString(),
        type,
        valueObj.path("tenant").asString(null),
        exportTypeSpecificParameters.toString(),
        valueObj.path("scheduleFrequency").asIntOpt().stream().boxed().findFirst().orElse(null),
        valueObj.path("schedulePeriod").asString(null),
        valueObj.path("scheduleTime").asString(null),
        weekDays.isMissingNode() ? null : weekDays.toString(),
        metadata.path("createdDate").asString(null),
        metadata.path("createdByUserId").asString(null),
        metadata.path("updatedDate").asString(null),
        metadata.path("updatedByUserId").asString(null)
      );
      log.info("migrateConfigEntry:: Successfully migrated export config with id: {}", id);
    } catch (Exception e) {
      log.error("migrateConfigEntry:: Failed to insert export config with id: {}", id, e);
    }
  }


  /**
   * Validates the exportTypeSpecificParameters by attempting to convert it to the ExportTypeSpecificParameters class.

   * @param exportTypeSpecificParameters the JsonNode containing the export type specific parameters to validate
   */
  private boolean validateExportTypeSpecificParams(JsonNode exportTypeSpecificParameters) {
    if (exportTypeSpecificParameters.isMissingNode()) {
      return false;
    }
    try {
      objectMapper.treeToValue(exportTypeSpecificParameters, ExportTypeSpecificParameters.class);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
