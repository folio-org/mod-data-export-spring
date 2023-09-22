package org.folio.des.service.config.acquisition;

import java.util.Optional;
import java.util.UUID;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EdifactOrdersExportService extends BaseExportConfigService {

  private final ExportJobScheduler exportJobScheduler;

  public EdifactOrdersExportService(ConfigurationClient client,
                                    DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
                                    ExportConfigConverterResolver exportConfigConverterResolver,
                                    ExportConfigValidatorResolver exportConfigValidatorResolver,
                                    ExportJobScheduler exportJobScheduler) {
    super(client, defaultModelConfigToExportConfigConverter, exportConfigConverterResolver, exportConfigValidatorResolver);
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
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    if (exportConfig.getId() == null) {
      exportConfig.setId(UUID.randomUUID().toString());
    }
    setExportConfigId(exportConfig);
    ModelConfiguration result = super.postConfig(exportConfig);
    exportJobScheduler.scheduleExportJob(exportConfig);
    log.info("postConfig:: initial jobs prepared for export config id '{}'", exportConfig.getId());
    return result;
  }

  private void setExportConfigId(ExportConfig exportConfig) {
    Optional.ofNullable(exportConfig.getExportTypeSpecificParameters())
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .ifPresent(ediOrdersExportConfig -> ediOrdersExportConfig.setExportConfigId(UUID.fromString(exportConfig.getId())));
  }
}
