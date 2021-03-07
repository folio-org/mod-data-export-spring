package org.folio.des.controller;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.model.response.ResponseErrors;
import org.folio.spring.utility.ErrorUtility;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Log4j2
public class ControllerExceptionHandler {

  @ExceptionHandler({ IllegalArgumentException.class, javax.validation.ConstraintViolationException.class,
      HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class, FeignException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseErrors handleIllegalArgumentException(Exception exception) {
    log.error(exception.getMessage(), exception);
    return ErrorUtility.buildError(exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({ DataIntegrityViolationException.class, ConstraintViolationException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseErrors handleConstraintViolationException(Exception exception) {
    /*
     * In the case of Constraint Violation exceptions, we must reach the main cause of the wrapped in each other exceptions
     * It will be either SQLException or platform-specific exception like PSQLException and it should contain
     * all the details related to the original database error/exception
     */
    Throwable cause = exception.getCause();
    while (cause instanceof Exception && cause != exception) {
      exception = (Exception) cause;
      cause = exception.getCause();
    }
    log.error(exception.getMessage(), exception);
    return ErrorUtility.buildError(exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseErrors handleNotFoundException(NotFoundException exception) {
    log.error(exception.getMessage(), exception);
    return ErrorUtility.buildError(exception, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseErrors handleException(Exception exception) {
    log.error(exception.getMessage(), exception);
    return ErrorUtility.buildError(exception, HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
