package org.folio.des.validator.acquisition;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.folio.des.domain.dto.ScheduleParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.Errors;

@SpringBootTest(classes = { EdifactOrdersScheduledParamsValidator.class})
public class EdifactOrdersScheduledParamsValidatorTest {
  @Autowired
  private EdifactOrdersScheduledParamsValidator validator;

  @Test
  @DisplayName("Should throw exception if scheduled time is Null")
  void shouldThrowExceptionIfScheduledTimeIsNull() {
    Errors errors = mock(Errors.class);
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    assertThrows(IllegalArgumentException.class, () ->  validator.validate(scheduleParameters, errors));
  }

  @Test
  @DisplayName("Should pass validation if scheduled time is correct")
  void shouldPassValidationIfScheduledTimeIsActive() {
    Errors errors = mock(Errors.class);
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setScheduleTime("12:34:56");
    validator.validate(scheduleParameters, errors);
  }

  @Test
  @DisplayName("Should throw exception if scheduled time is incorrect format")
  void shouldThrowExceptionIfBursarFeeFinesIsNull() {
    Errors errors = mock(Errors.class);
    ScheduleParameters scheduleParameters = new ScheduleParameters();
    scheduleParameters.setScheduleTime("12:34:56+08:00");
    assertThrows(IllegalArgumentException.class, () ->  validator.validate(scheduleParameters, errors));
  }
}
