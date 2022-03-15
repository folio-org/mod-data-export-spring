package org.folio.des.service.config.acquisition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.converter.ExportConfigConverterResolver;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.scheduling.acquisition.EdifactOrdersExportJobScheduler;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.validator.ExportConfigValidatorResolver;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EdifactOrdersExportService extends BaseExportConfigService {

  private final EdifactOrdersExportJobScheduler exportJobScheduler;

  public EdifactOrdersExportService(ConfigurationClient client,
                                    DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter,
                                    ExportConfigConverterResolver exportConfigConverterResolver,
                                    ExportConfigValidatorResolver exportConfigValidatorResolver,
                                    EdifactOrdersExportJobScheduler exportJobScheduler) {
    super(client, defaultModelConfigToExportConfigConverter, exportConfigConverterResolver, exportConfigValidatorResolver);
    this.exportJobScheduler = exportJobScheduler;
  }

  @Override
  public void updateConfig(String configId, ExportConfig exportConfig) {
    setExportConfigId(exportConfig);
    super.updateConfig(configId, exportConfig);
    List<Job> scheduledJobs = exportJobScheduler.scheduleExportJob(exportConfig);
    scheduledJobs.forEach(scheduledJob -> log.info("Job re-scheduled: {}", scheduledJob.getId()));
  }

  @Override
  public ModelConfiguration postConfig(ExportConfig exportConfig) {
    if (exportConfig.getId() == null) {
      exportConfig.setId(UUID.randomUUID().toString());
    }
    setExportConfigId(exportConfig);
    ModelConfiguration result = super.postConfig(exportConfig);
    List<Job> scheduledJobs = exportJobScheduler.scheduleExportJob(exportConfig);
    scheduledJobs.forEach(scheduledJob -> log.info("InitialJob prepared: {}", scheduledJob));
    return result;
  }

  private void setExportConfigId(ExportConfig exportConfig) {
    Optional.ofNullable(exportConfig.getExportTypeSpecificParameters())
      .map(ExportTypeSpecificParameters::getVendorEdiOrdersExportConfig)
      .ifPresent(ediOrdersExportConfig -> ediOrdersExportConfig.setExportConfigId(UUID.fromString(exportConfig.getId())));
  }
}
