package org.folio.des.mapper.acquisition;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.folio.des.mapper.BaseExportConfigMapper;
import org.folio.des.service.config.ExportConfigConstants;
import org.folio.des.validator.acquisition.ClaimsExportParametersValidator;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;

@Mapper(imports = {ExportConfigConstants.class, ExportTypeSpecificParameters.class, ExportTypeSpecificParametersWithLegacyBursar.class})
public abstract class ClaimsExportConfigMapper extends BaseExportConfigMapper {

  @Autowired
  private ClaimsExportParametersValidator validator;

  @BeforeMapping
  protected void validateConfig(ExportConfig exportConfig) {
    var errors = new BeanPropertyBindingResult(exportConfig.getExportTypeSpecificParameters(), "specificParameters");
    validator.validate(exportConfig.getExportTypeSpecificParameters(), errors);
  }

  @Override
  protected String getConfigName(ExportConfig exportConfig) {
    var ediOrdersExportConfig = exportConfig.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig();
    return "%s_%s_%s".formatted(
      exportConfig.getType().getValue(),
      ediOrdersExportConfig.getVendorId(),
      exportConfig.getId());
  }

}
