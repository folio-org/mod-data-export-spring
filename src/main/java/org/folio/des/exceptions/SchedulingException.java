package org.folio.des.exceptions;

public class SchedulingException extends RuntimeException {
  public SchedulingException(String message, Exception cause) {
    super(message, cause);
  }
}
