package org.folio.des.service.config.acquisition;

import java.util.Optional;
import java.util.UUID;

import org.folio.des.mapper.BaseExportConfigMapper;
import org.folio.des.mapper.DefaultExportConfigMapper;
import org.folio.des.mapper.ExportConfigMapperResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EdifactOrdersExportService extends BaseExportConfigService {

  private final ExportJobScheduler exportJobScheduler;

  public EdifactOrdersExportService(ExportConfigRepository repository, BaseExportConfigMapper defaultExportConfigMapper,
                                    ExportConfigMapperResolver exportConfigMapperResolver, ExportConfigValidatorResolver exportConfigValidatorResolver,
                                    ExportJobScheduler exportJobScheduler) {
    super(repository, defaultExportConfigMapper, exportConfigMapperResolver, exportConfigValidatorResolver);
    this.exportJobScheduler = exportJobScheduler;
  }

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    setExportConfigId(exportConfig);
    super.updateConfig(configId, exportConfig);
    exportJobScheduler.scheduleExportJob(exportConfig);
    log.info("updateConfig:: jobs rescheduled for export config id='{}'", exportConfig.getId());
  }

  @Override
  public ExportConfig postConfig(ExportConfig exportConfig) {
    if (exportConfig.getId() == null) {
      exportConfig.setId(UUID.randomUUID().toString());
    }
    setExportConfigId(exportConfig);
    log.debug("postConfig:: by exportConfig={}", exportConfig);
    exportConfig = super.postConfig(exportConfig);
    exportJobScheduler.scheduleExportJob(exportConfig);
    log.info("postConfig:: initial jobs prepared for export config id '{}'", exportConfig.getId());
    return exportConfig;
  }

  private void setExportConfigId(ExportConfig exportConfig) {
    Optional.ofNullable(exportConfig.getExportTypeSpecificParameters())
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .ifPresent(ediOrdersExportConfig -> ediOrdersExportConfig.setExportConfigId(UUID.fromString(exportConfig.getId())));
  }
}
