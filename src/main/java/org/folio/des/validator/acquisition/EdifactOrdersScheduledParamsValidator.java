package org.folio.des.validator.acquisition;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ScheduleParameters;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Service
public class EdifactOrdersScheduledParamsValidator implements Validator {
  @Override
  public boolean supports(Class<?> aClass) {
    return ScheduleParameters.class.isAssignableFrom(aClass);
  }

  @Override
  public void validate(Object target, Errors errors) {
    ScheduleParameters specificParameters = (ScheduleParameters) target;
    if (specificParameters.getScheduleTime() == null) {
      String msg = String.format("%s scheduled parameters should contain scheduled time", ExportType.EDIFACT_ORDERS_EXPORT.getValue());
      throw new IllegalArgumentException(msg);
    }
    try {
      LocalTime.parse(specificParameters.getScheduleTime(), DateTimeFormatter.ISO_LOCAL_TIME);
    } catch (Exception e) {
      String msg = String.format("Scheduled parameters should contain scheduled time in format %s:", "HH:mm:ss (12:45:34)");
      throw new IllegalArgumentException(msg);
    }
  }
}
