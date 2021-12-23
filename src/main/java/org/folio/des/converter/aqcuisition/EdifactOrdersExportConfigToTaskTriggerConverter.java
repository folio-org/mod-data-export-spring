package org.folio.des.converter.aqcuisition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.acquisition.OrgConfig;
import org.folio.des.scheduling.acquisition.AcqBaseExportTaskTrigger;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Service
public class EdifactOrdersExportConfigToTaskTriggerConverter implements Converter<ExportConfig, List<ExportTaskTrigger>>  {
  private static final String CONFIG_QUERY = "module==ORG and configName==localeSettings";
  public static final String LOCALE_SETTINGS = "localeSettings";

  public static final String SYSTEM_CONFIG_MODULE_NAME = "ORG";
  public static final String CURRENCY_CONFIG = "currency";
  public static final String DEFAULT_CURRENCY = "USD";

  public static final String TZ_CONFIG = "timezone";
  public static final String TZ_UTC = "UTC";

  private EdifactOrdersExportParametersValidator validator;
  private ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;

  @Override
  public List<ExportTaskTrigger> convert(ExportConfig exportConfig) {
    ExportTypeSpecificParameters specificParameters = exportConfig.getExportTypeSpecificParameters();
    Errors errors = new BeanPropertyBindingResult(specificParameters, "specificParameters");
    validator.validate(specificParameters, errors);
    if (ExportConfig.SchedulePeriodEnum.NONE != exportConfig.getSchedulePeriod()) {
      List<ExportTaskTrigger> exportTaskTriggers = new ArrayList<>();
      Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
              .map(VendorEdiOrdersExportConfig::getEdiSchedule)
              .filter(EdiSchedule::getEnableScheduledExport)
              .map(EdiSchedule::getScheduleParameters)
              .ifPresent(scheduleParameters -> {
                if (scheduleParameters.getId() == null) {
                  scheduleParameters.setId(UUID.randomUUID());
                }
                exportTaskTriggers.add(new AcqBaseExportTaskTrigger(scheduleParameters));
              });
      return exportTaskTriggers;
    }
    return Collections.emptyList();
  }

  public String getSystemTimeZone() {
    ConfigurationCollection configurationCollection = configurationClient.getConfigurations(CONFIG_QUERY);
    Optional<ModelConfiguration> optConfig = configurationCollection.getConfigs().stream().findFirst();

    if (optConfig.isPresent() && optConfig.get().getValue() != null) {
      ModelConfiguration config = optConfig.get();
      try {
        var orgConfig = objectMapper.readValue(config.getValue(), OrgConfig.class);
        return orgConfig.getTimezone();
      } catch (JsonProcessingException e) {
        return TZ_UTC;
      }
    }
    return TZ_UTC;
  }

  private String extractLocalSettingConfigValueByName(JsonObject config, String name, String defaultValue) {
    String localeSettings = config.getString(LOCALE_SETTINGS);
    String confValue;
    if (StringUtils.isEmpty(localeSettings)) {
      confValue = defaultValue;
    } else {
      confValue = new JsonObject(config.getString(LOCALE_SETTINGS)).getString(name, defaultValue);
    }
    return confValue;
  }

}
