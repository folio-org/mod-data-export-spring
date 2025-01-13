package org.folio.des.service.config.impl;

import static org.folio.des.domain.FilterOperator.AND_OPERATOR;
import static org.folio.des.domain.FilterOperator.OR_OPERATOR;
import static org.folio.des.domain.FilterPredicate.BY_TYPE_CONDITION;
import static org.folio.des.domain.FilterPredicate.BY_VALUE_CONDITION;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_QUERY;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
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

@Log4j2
@AllArgsConstructor
public class ExportTypeBasedConfigManager {

  public static final String EXPORT_CONFIGURATION_NOT_FOUND = "Export configuration not found or parse error : %s";
  private static final int EXPORT_TYPE = 0;

  private final Pattern exportTypePattern = Pattern.compile(EnumSet.allOf(ExportType.class).stream()
    .map(ExportType::toString).collect(Collectors.joining("|")));

  private final ConfigurationClient client;
  private final ExportConfigServiceResolver exportConfigServiceResolver;
  private final ExportConfigService defaultExportConfigService;
  private final DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;

  public void updateConfig(String configId, ExportConfig exportConfig) {
    log.info("updateConfig:: configId={}", configId);
    if (exportConfig.getId() == null || !exportConfig.getId().equals(configId)) {
      log.error(ErrorCodes.MISMATCH_BETWEEN_ID_IN_PATH_AND_BODY.getDescription());
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
    log.info("postConfig:: by exportConfig={}", exportConfig);
    Optional<ExportConfigService> exportConfigService = exportConfigServiceResolver.resolve(exportConfig.getType());
    if (exportConfigService.isPresent()) {
      return exportConfigService.get().postConfig(exportConfig);
    }
    log.info("postConfig:: defaultExportConfigService.postConfig flow's working.");
    return defaultExportConfigService.postConfig(exportConfig);
  }

  public ExportConfigCollection getConfigCollection(String query, Integer limit) {
    var exportTypes = extractExportTypes(query);
    if (isFilterByType(query) && CollectionUtils.isNotEmpty(exportTypes)) {
      log.info("getConfigCollection:: exportTypes is non-empty");
      return getConfigCollectionByExportTypes(limit, exportTypes);
    }
    var normalizedQuery = normalizeQuery(exportTypes, query);
    log.info("getConfigCollection:: defaultExportConfigService.getConfigCollection flow's working, by normalizedQuery: {} with limit={}", normalizedQuery, limit);
    return defaultExportConfigService.getConfigCollection(normalizedQuery, limit);
  }

  private boolean isFilterByType(String query) {
    return StringUtils.isNotEmpty(query) && query.startsWith(BY_TYPE_CONDITION.getValue());
  }

  private ExportConfigCollection getConfigCollectionByExportTypes(Integer limit, List<ExportType> exportTypes) {
    var exportConfigCollections = collectConfigsByService(limit, exportTypes);
    var mergedConfigs = exportConfigCollections.stream()
      .flatMap(configs -> configs.getConfigs().stream()).toList();
    return new ExportConfigCollection().configs(mergedConfigs).totalRecords(mergedConfigs.size());
  }

  private List<ExportConfigCollection> collectConfigsByService(Integer limit, List<ExportType> exportTypes) {
    var exportConfigCollections = new ArrayList<ExportConfigCollection>();
    for (ExportType exportType : exportTypes) {
      var normalizedQuery = normalizeQuery(exportType);
      log.info("collectConfigsByService:: by normalizedQuery: {} with limit={}", normalizedQuery, limit);
      if (exportConfigServiceResolver.resolve(exportType).isPresent()) {
        var configService = exportConfigServiceResolver.resolve(exportType).get();
        exportConfigCollections.add(configService.getConfigCollection(normalizedQuery, limit));
      } else {
        log.info("collectConfigsByService:: defaultExportConfigService.getConfigCollection flow's working, by normalizedQuery: {} with limit={}", normalizedQuery, limit);
        exportConfigCollections.add(defaultExportConfigService.getConfigCollection(normalizedQuery, limit));
      }
    }
    return exportConfigCollections;
  }

  private List<ExportType> extractExportTypes(String query) {
    if (StringUtils.isEmpty(query)) {
      return List.of();
    }
    var exportTypes = new ArrayList<ExportType>();
    for (var entry : query.split(OR_OPERATOR.getValue())) {
      var matcher = exportTypePattern.matcher(entry);
      if (!matcher.find()) {
        continue;
      }
      var exportTypeString = matcher.group(EXPORT_TYPE);
      if (Objects.nonNull(exportTypeString)) {
        exportTypes.add(ExportType.valueOf(exportTypeString));
      }
    }
    return exportTypes;
  }

  private String normalizeQuery(ExportType exportType) {
    return String.format("%s AND value==*%s*", DEFAULT_MODULE_QUERY, exportType);
  }

  private String normalizeQuery(List<ExportType> exportTypes, String query) {
    if (StringUtils.isEmpty(query)) {
      return DEFAULT_MODULE_QUERY;
    }
    if (!query.contains(DEFAULT_MODULE_NAME)) {
      query = DEFAULT_MODULE_QUERY + AND_OPERATOR.getValue() + query;
    }
    if (isFilterByType(query) && CollectionUtils.isNotEmpty(exportTypes)) {
      query = query.replace(BY_TYPE_CONDITION.getValue(), BY_VALUE_CONDITION.getValue());
      for (ExportType exportType : exportTypes) {
        query = query.replaceAll(exportType.name(), String.format("*%s*", exportType));
      }
    }
    return query;
  }

  public ExportConfig getConfigById(String exportConfigId) {
    var configuration = client.getConfigById(exportConfigId);
    if (configuration == null) {
      log.error("Export configuration not found or parse error : {}}", exportConfigId);
      throw new NotFoundException(String.format(EXPORT_CONFIGURATION_NOT_FOUND, exportConfigId));
    }
    ExportConfig exportConfig = defaultModelConfigToExportConfigConverter.convert(configuration);
    log.info("getConfigById:: result={}.", exportConfigId);
    return exportConfig;
  }

  public void deleteConfigById(String exportConfigId) {
    client.deleteConfigById(exportConfigId);
  }
}
