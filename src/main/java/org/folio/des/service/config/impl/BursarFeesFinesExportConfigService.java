package org.folio.des.service.config.impl;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_QUERY;

import java.util.Optional;
import java.util.function.Function;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.mapper.BaseExportConfigMapper;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.domain.dto.ExportConfigWithLegacyBursar;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigCollection;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.springframework.data.domain.PageRequest;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BursarFeesFinesExportConfigService extends BaseExportConfigService {

  private final BursarExportScheduler bursarExportScheduler;

  public BursarFeesFinesExportConfigService(ExportConfigRepository repository, DefaultExportConfigMapper defaultExportConfigMapper,
                                            ExportConfigMapperResolver exportConfigMapperResolver, ExportConfigValidatorResolver exportConfigValidatorResolver,
                                            BursarExportScheduler bursarExportScheduler) {
    super(repository, defaultExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver);
    this.bursarExportScheduler = bursarExportScheduler;
  }

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    log.info("updateConfig:: starting Bursar updateConfig with configId:{}", configId);
    super.updateConfig(configId, exportConfig);
    bursarExportScheduler.scheduleBursarJob(exportConfig);
  }

  @Override
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    log.info("postConfig:: starting Bursar postConfig with configId:{}", exportConfig.getId());
    ModelConfiguration modelConfiguration = super.postConfig(exportConfig);
    bursarExportScheduler.scheduleBursarJob(exportConfig);
    return modelConfiguration;
  }

  @Override
  public ExportConfigCollection getConfigCollection(String query, Integer limit) {
    log.info("getConfigCollection:: the result doesn't base on a query.");
    return getFirstConfig().map(this::createExportConfigCollection)
      .orElse(emptyExportConfigCollection());
  }

  @Override
  public Optional<ExportConfig> getFirstConfig() {
    return getFirstConfigGeneric(defaultExportConfigMapper::toDto);
  }

  public Optional<ExportConfigWithLegacyBursar> getFirstConfigLegacy() {
    return getFirstConfigGeneric(defaultExportConfigMapper::toDtoLegacy);
  }

  private  <T> Optional<T> getFirstConfigGeneric(Function<ExportConfigEntity, T> converter) {
    return repository.findByCql(DEFAULT_CONFIG_QUERY.formatted(DEFAULT_CONFIG_NAME), PageRequest.of(0, 1))
      .stream().findFirst()
      .map(converter);
  }

  private ExportConfigCollection createExportConfigCollection(ExportConfig exportConfig) {
    var configCollection = new ExportConfigCollection();
    configCollection.addConfigsItem(exportConfig);
    configCollection.setTotalRecords(1);
    log.info("createExportConfigCollection:: configCollection={}.", configCollection);
    return configCollection;
  }

  private ExportConfigCollection emptyExportConfigCollection() {
    log.debug("emptyExportConfigCollection:: ");
    var configCollection = new ExportConfigCollection();
    configCollection.setTotalRecords(0);
    return configCollection;
  }
}
