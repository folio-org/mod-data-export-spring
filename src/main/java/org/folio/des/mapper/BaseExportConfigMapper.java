package org.folio.des.mapper;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigWithLegacyBursar;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.folio.des.service.config.ExportConfigConstants;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.databind.ObjectMapper;

@Mapper(implementationName = "DefaultExportConfigMapper", imports = {ExportConfigConstants.class, ExportTypeSpecificParameters.class, ExportTypeSpecificParametersWithLegacyBursar.class})
public abstract class BaseExportConfigMapper {

  @Autowired
  @Qualifier("entityObjectMapper")
  protected ObjectMapper objectMapper;

  @Mapping(target = "configName", expression = "java(getConfigName(dto))")
  @Mapping(target = "exportTypeSpecificParameters", expression = "java(objectMapper.convertValue(dto.getExportTypeSpecificParameters(), ExportTypeSpecificParameters.class))")
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  public abstract ExportConfigEntity toEntity(ExportConfig dto);

  @Mapping(target = "exportTypeSpecificParameters", expression = "java(objectMapper.convertValue(entity.getExportTypeSpecificParameters(), ExportTypeSpecificParameters.class))")
  public abstract ExportConfig toDto(ExportConfigEntity entity);

  @Mapping(target = "exportTypeSpecificParameters", expression = "java(objectMapper.convertValue(entity.getExportTypeSpecificParameters(), ExportTypeSpecificParametersWithLegacyBursar.class))")
  public abstract ExportConfigWithLegacyBursar toDtoLegacy(ExportConfigEntity entity);

  protected String getConfigName(ExportConfig exportConfig) {
    return exportConfig.getType() == null || ExportType.BURSAR_FEES_FINES.equals(exportConfig.getType())
      ? DEFAULT_CONFIG_NAME
      : exportConfig.getType().getValue();
  }

}
