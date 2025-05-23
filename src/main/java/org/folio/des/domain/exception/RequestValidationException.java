package org.folio.des.domain.exception;

import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.Error;
import org.folio.des.domain.dto.ErrorType;

public class RequestValidationException extends RuntimeException {
  private final transient Error error;

  public RequestValidationException(String message) {
    super(StringUtils.isNotEmpty(message) ? message : ErrorCodes.GENERIC_ERROR_CODE.getDescription());
    this.error = new Error();
    error.setType(ErrorType.ERROR);
    error.setCode(ErrorCodes.GENERIC_ERROR_CODE.getCode());
    error.setMessage(RequestValidationException.class.getSimpleName() + ": " + message);
  }

  public RequestValidationException(ErrorCodes errCodes) {
    super(errCodes.getDescription());
    this.error = new Error();
    error.setCode(errCodes.getCode());
    error.setMessage(errCodes.getDescription());
  }

  public RequestValidationException(Error error) {
    this.error = error;
  }

  public Error getError() {
    return error;
  }
}
