package org.folio.des.controller;

import java.util.ArrayList;
import java.util.List;

import org.folio.des.domain.dto.Error;
import org.folio.des.domain.dto.Errors;
import org.folio.des.domain.exception.RequestValidationException;
import org.folio.spring.exception.NotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class ControllerExceptionHandler {

  @ExceptionHandler({
    IllegalArgumentException.class,
    javax.validation.ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    MethodArgumentNotValidException.class,
    FeignException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Errors handleIllegalArgumentException(Exception exception) {
    return buildError(exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RequestValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Errors handleExportRequestValidationException(RequestValidationException exception) {
    List<Error> listOfErrors = new ArrayList<>();
    listOfErrors.add(exception.getError());
    var errors = new Errors();
    errors.setErrors(listOfErrors);
    errors.setTotalRecords(listOfErrors.size());
    return errors;
  }

  @ExceptionHandler({DataIntegrityViolationException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Errors handleConstraintViolationException(Exception exception) {
    /*
     * In the case of Constraint Violation exceptions, we must reach the main cause of the wrapped in each other exceptions
     * It will be either SQLException or platform-specific exception like PSQLException and it should contain
     * all the details related to the original database error/exception
     */
    Throwable cause = exception.getCause();
    while (cause instanceof Exception exception1 && cause != exception) {
      exception = exception1;
      cause = exception.getCause();
    }
    return buildError(exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Errors handleNotFoundException(NotFoundException exception) {
    return buildError(exception, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Errors handleException(Exception exception) {
    return buildError(exception, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private Errors buildError(Exception exception, HttpStatus status) {
    log.error(exception.getMessage(), exception);
    return buildError(exception.getLocalizedMessage(), exception.getClass().getSimpleName(), status.toString());
  }

  private Errors buildError(String message, String type, String code) {
    var error = new Error();
    error.setMessage(message);
    error.setType(type);
    error.setCode(code);
    List<Error> listOfErrors = new ArrayList<>();
    listOfErrors.add(error);
    return buildErrors(listOfErrors);
  }

  private Errors buildErrors(List<Error> listOfErrors) {
    var errors = new Errors();
    errors.setErrors(listOfErrors);
    errors.setTotalRecords(listOfErrors.size());
    return errors;
  }

}
