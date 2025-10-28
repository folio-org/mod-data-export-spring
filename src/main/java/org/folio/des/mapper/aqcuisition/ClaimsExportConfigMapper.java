package org.folio.des.mapper.aqcuisition;

import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.folio.des.mapper.BaseExportConfigMapper;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.service.config.ExportConfigConstants;
import org.folio.des.validator.acquisition.ClaimsExportParametersValidator;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;

@Mapper(imports = {ExportConfigConstants.class, ExportTypeSpecificParameters.class, ExportTypeSpecificParametersWithLegacyBursar.class})
public abstract class ClaimsExportConfigMapper extends BaseExportConfigMapper {

  private static final String CONFIG_DESCRIPTION = "Claims export configuration parameters";

  @Autowired
  private ClaimsExportParametersValidator validator;

  protected String getConfigName(ExportConfig exportConfig) {
    var errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
    validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);

    var ediOrdersExportConfig = exportConfig.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig();
    return exportConfig.getType().getValue() + "_" + ediOrdersExportConfig.getVendorId().toString() + "_" + exportConfig.getId();
  }

  protected String getConfigDescription() {
    return CONFIG_DESCRIPTION;
  }

}
