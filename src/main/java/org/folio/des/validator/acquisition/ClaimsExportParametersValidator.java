package org.folio.des.validator.acquisition;

import org.folio.des.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum;
import org.springframework.stereotype.Service;

@Service
public class ClaimsExportParametersValidator extends AbstractExportParametersValidator {

  @Override
  protected IntegrationTypeEnum getExpectedIntegrationType() {
    return IntegrationTypeEnum.CLAIMING;
  }

}
