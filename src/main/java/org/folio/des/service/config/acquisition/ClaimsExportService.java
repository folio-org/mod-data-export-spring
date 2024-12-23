package org.folio.des.service.config.acquisition;

import lombok.extern.log4j.Log4j2;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;

import java.util.Optional;
import java.util.UUID;

@Log4j2
public class ClaimsExportService extends BaseExportConfigService {

  public ClaimsExportService(ConfigurationClient client,
                             DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
                             ExportConfigConverterResolver exportConfigConverterResolver,
                             ExportConfigValidatorResolver exportConfigValidatorResolver) {
    super(client, defaultModelConfigToExportConfigConverter, exportConfigConverterResolver, exportConfigValidatorResolver);
  }

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    setExportConfigId(exportConfig);
    super.updateConfig(configId, exportConfig);
    log.info("updateConfig:: jobs rescheduled for export config id='{}'", exportConfig.getId());
  }

  @Override
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    if (exportConfig.getId() == null) {
      exportConfig.setId(UUID.randomUUID().toString());
    }
    setExportConfigId(exportConfig);
    log.debug("postConfig:: by exportConfig={}", exportConfig);
    ModelConfiguration result = super.postConfig(exportConfig);
    log.info("postConfig:: initial jobs prepared for export config id '{}'", exportConfig.getId());
    return result;
  }

  private void setExportConfigId(ExportConfig exportConfig) {
    Optional.ofNullable(exportConfig.getExportTypeSpecificParameters())
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .ifPresent(ediOrdersExportConfig -> ediOrdersExportConfig.setExportConfigId(UUID.fromString(exportConfig.getId())));
  }
}
