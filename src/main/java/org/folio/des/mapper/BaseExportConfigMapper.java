package org.folio.des.mapper;

import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_CONFIG_NAME;

import java.util.Optional;

import org.folio.de.entity.ExportConfigEntity;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigWithLegacyBursar;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.folio.des.domain.dto.Metadata;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.service.config.ExportConfigConstants;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

@Mapper(imports = {ExportConfigConstants.class, ExportTypeSpecificParameters.class, ExportTypeSpecificParametersWithLegacyBursar.class}, implementationName = "DefaultExportConfigMapper")
public abstract class BaseExportConfigMapper {

  private static final String CONFIG_DESCRIPTION = "Data export configuration parameters";

  @Autowired
  protected ObjectMapper objectMapper;

  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  public abstract ExportConfigEntity toEntity(ExportConfig dto);

  @Mapping(target = "exportTypeSpecificParameters", expression = "java(objectMapper.convertValue(entity.getExportTypeSpecificParameters(), ExportTypeSpecificParameters.class))")
  public abstract ExportConfig toDto(ExportConfigEntity entity);

  @Mapping(target = "exportTypeSpecificParameters", expression = "java(objectMapper.convertValue(entity.getExportTypeSpecificParameters(), ExportTypeSpecificParametersWithLegacyBursar.class))")
  public abstract ExportConfigWithLegacyBursar toDtoLegacy(ExportConfigEntity entity);

  @Mapping(target = "module", expression = "java(ExportConfigConstants.DEFAULT_MODULE_NAME)")
  @Mapping(target = "configName", expression = "java(getConfigName(dto))")
  @Mapping(target = "description", expression = "java(getConfigDescription())")
  @Mapping(target = "enabled", constant = "true")
  @Mapping(target = "default", constant = "true")
  @Mapping(target = "value", expression = "java(objectMapper.writeValueAsString(dto))")
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "_default", ignore = true)
  @Mapping(target = "metadata", ignore = true)
  public abstract ModelConfiguration toModelConfiguration(ExportConfig dto) throws JsonProcessingException;

  @SneakyThrows
  public ModelConfiguration toModelConfiguration(ExportConfigEntity entity) {
    var config = toModelConfiguration(toDto(entity));
    updateDtoMetadata(Optional.ofNullable(config.getMetadata()).orElseGet(() -> config.metadata(new Metadata()).getMetadata()), entity);
    return config;
  }

  @Mapping(target = "createdDate", source = "createdDate")
  @Mapping(target = "createdByUserId", source = "createdBy")
  @Mapping(target = "updatedDate", source = "updatedDate")
  @Mapping(target = "updatedByUserId", source = "updatedBy")
  public abstract void updateDtoMetadata(@MappingTarget Metadata configuration, ExportConfigEntity entity);

  protected String getConfigName(ExportConfig exportConfig) {
    return exportConfig.getType() == null || ExportType.BURSAR_FEES_FINES.equals(exportConfig.getType())
      ? DEFAULT_CONFIG_NAME
      : exportConfig.getType().getValue();
  }

  protected String getConfigDescription() {
    return CONFIG_DESCRIPTION;
  }

}
