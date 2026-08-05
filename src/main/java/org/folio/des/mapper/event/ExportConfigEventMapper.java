package org.folio.des.mapper.event;

import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.event.EdiFtpEventDto;
import org.folio.des.domain.dto.event.ExportConfigEventDto;
import org.folio.des.domain.dto.event.ExportTypeSpecificParametersEventDto;
import org.folio.des.domain.dto.event.VendorEdiOrdersExportConfigEventDto;
import org.mapstruct.Mapper;

/**
 * Maps the REST {@link ExportConfig} onto the redacted {@link ExportConfigEventDto} published on the event bus.
 *
 * <p>Redaction is structural: the {@link EdiFtpEventDto} target simply has no {@code password} field, so the
 * source {@code EdiFtp.password} is left unmapped and can never reach the payload.
 */
@Mapper(componentModel = "spring")
public interface ExportConfigEventMapper {

  ExportConfigEventDto toEventDto(ExportConfig exportConfig);

  ExportTypeSpecificParametersEventDto toEventDto(ExportTypeSpecificParameters parameters);

  VendorEdiOrdersExportConfigEventDto toEventDto(VendorEdiOrdersExportConfig config);

  EdiFtpEventDto toEventDto(EdiFtp ediFtp);
}
