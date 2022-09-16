package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_QUERY;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.exception.ErrorCodes;
import org.folio.des.domain.exception.RequestValidationException;
import org.folio.des.service.config.ExportConfigService;
import org.folio.spring.exception.NotFoundException;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Log4j2
@Lazy
@Component
public class ExportTypeBasedConfigManager {
  public static final String EXPORT_CONFIGURATION_NOT_FOUND = "Export configuration not found or parse error : %s";
  private static final int EXPORT_TYPE = 1;

  private final ConfigurationClient client;
  private final ExportConfigServiceResolver exportConfigServiceResolver;
  private final ExportConfigService defaultExportConfigService;
  private final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  private final Pattern exportTypePattern = Pattern.compile("(type==" + EnumSet.allOf(ExportType.class).stream()
                                                                    .map(ExportType::toString)
                                                                    .collect(Collectors.joining("|")) + ")");

  public void updateConfig(String configId, ExportConfig exportConfig) {
    if (exportConfig.getId() == null || !exportConfig.getId().equals(configId)) {
      throw new RequestValidationException(ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY);
    }
    exportConfigServiceResolver.resolve(exportConfig.getType())
            .ifPresentOrElse(service -> service.updateConfig(configId, exportConfig),
              () -> defaultExportConfigService.updateConfig(configId, exportConfig));
  }

  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    if (exportConfig.getId() == null) {
      exportConfig.setId(UUID.randomUUID().toString());
    }
    Optional<ExportConfigService> exportConfigService = exportConfigServiceResolver.resolve(exportConfig.getType());
    if (exportConfigService.isPresent()) {
      return exportConfigService.get().postConfig(exportConfig);
    }
    return defaultExportConfigService.postConfig(exportConfig);
  }

  public ExportConfigCollection getConfigCollection(String query, Integer limit) {
    Optional<ExportType> exportTypeOpt = extractExportType(query);
    String normalizedQuery = normalizeQuery(exportTypeOpt, query);
    if (exportTypeOpt.isPresent()) {
      Optional<ExportConfigService> exportConfigService = exportConfigServiceResolver.resolve(exportTypeOpt.get());
      if (exportConfigService.isPresent()) {
        return exportConfigService.get().getConfigCollection(normalizedQuery, limit);
      }
    }
    return defaultExportConfigService.getConfigCollection(normalizedQuery, limit);
  }

  public ExportConfig getConfigById(String exportConfigId) {
    var configuration = client.getConfigById(exportConfigId);

    if (configuration == null) {
      throw new NotFoundException(String.format(EXPORT_CONFIGURATION_NOT_FOUND, exportConfigId));
    }
    return defaultModelConfigToExportConfigConverter.convert(configuration);
  }

  public void deleteConfigById(String exportConfigId) {
    client.deleteConfigById(exportConfigId);
  }

  private Optional<ExportType> extractExportType(String query) {
    if (StringUtils.isNotEmpty(query)) {
      Matcher matcher = exportTypePattern.matcher(query);
      if (matcher.find()) {
        String exportTypeString = matcher.group(EXPORT_TYPE);
        return Optional.ofNullable(exportTypeString).map(ExportType::valueOf);
      }
    }
    return Optional.empty();
  }

  private String normalizeQuery(Optional<ExportType> exportTypeOpt, String query) {
    if (query == null) {
      return DEFAULT_MODULE_QUERY;
    }
    if (!query.contains(DEFAULT_MODULE_NAME)) {
      query = DEFAULT_MODULE_QUERY + " and " + query;
    }
    if (exportTypeOpt.isPresent()) {
      String exportType = exportTypeOpt.get().getValue();
      query = query.replaceFirst(String.format("type*==*%s", exportType), String.format("value==*%s*", exportType));
    }
    return query;
  }
}
