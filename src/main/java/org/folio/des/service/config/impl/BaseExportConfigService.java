package org.folio.des.service.config.impl;

import static org.folio.des.service.config.impl.ExportTypeBasedConfigManager.EXPORT_CONFIGURATION_NOT_FOUND;

import java.util.Optional;
import java.util.UUID;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.service.config.ExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.spring.exception.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class BaseExportConfigService implements ExportConfigService {

  protected final ExportConfigRepository repository;
  protected final DefaultExportConfigMapper defaultExportConfigMapper;
  protected final ExportConfigMapperResolver exportConfigMapperResolver;
  protected final ExportConfigValidatorResolver exportConfigValidatorResolver;

  @Override
  @Transactional
  public void updateConfig(String configId, ExportConfig exportConfig) {
    log.info("updateConfig:: configId={}, exportConfig={}", configId, exportConfig);
    validateIncomingExportConfig(exportConfig);
    getExportConfigEntityOrThrow(configId);

    var entity = defaultExportConfigMapper.toEntity(exportConfig);
    repository.save(entity);
    log.info("updateConfig:: Successfully updated config with id={}", configId);
  }

  @Override
  @Transactional
  public ExportConfig postConfig(ExportConfig exportConfig) {
    log.info("postConfig:: exportConfig={}", exportConfig);
    validateIncomingExportConfig(exportConfig);

    var entity = defaultExportConfigMapper.toEntity(exportConfig);
    repository.save(entity);
    log.info("postConfig:: Successfully created config with id={}", exportConfig.getId());

    return toDto(entity);
  }

  @Override
  public ExportConfigCollection getConfigCollection(String query, Integer limit) {
    log.info("getConfigCollection:: query={}, limit={}", query, limit);
    var page = repository.findByCql(query, PageRequest.of(0, limit));
    return new ExportConfigCollection().totalRecords(page.getNumberOfElements())
      .configs(page.getContent().stream()
        .map(defaultExportConfigMapper::toDto)
        .toList());
  }

  @Override
  public Optional<ExportConfig> getFirstConfig() {
    return repository.findAll(PageRequest.of(0, 1))
      .getContent().stream()
      .map(defaultExportConfigMapper::toDto)
      .findFirst();
  }

  @Override
  public ExportConfig getConfigById(String configId) {
    return repository.findById(UUID.fromString(configId))
      .map(defaultExportConfigMapper::toDto)
      .orElseThrow(() -> new NotFoundException(EXPORT_CONFIGURATION_NOT_FOUND.formatted(configId)));
  }

  @Override
  public void deleteConfigById(String configId) {
    var config = getExportConfigEntityOrThrow(configId);
    repository.delete(config);
    log.info("deleteConfigById:: Successfully deleted config with id={}", configId);
  }

  @SneakyThrows
  protected ExportConfig toDto(ExportConfigEntity exportConfigEntity) {
    return exportConfigMapperResolver.resolve(exportConfigEntity.getType()).toDto(exportConfigEntity);
  }

  protected void validateIncomingExportConfig(ExportConfig exportConfig) {
    exportConfigValidatorResolver.resolve(exportConfig.getType(), ExportTypeSpecificParameters.class).ifPresent(validator -> {
      Errors errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
      validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);
    });
  }

  private ExportConfigEntity getExportConfigEntityOrThrow(String id) {
    return repository.findById(UUID.fromString(id))
      .orElseThrow(() -> new NotFoundException(String.format(EXPORT_CONFIGURATION_NOT_FOUND, id)));
  }

}
