package org.folio.des.config.feign;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.folio.spring.exception.NotFoundException;
import org.springframework.http.HttpStatus;

import static feign.FeignException.errorStatus;

public class CustomFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String requestUrl = response.request().url();
    if (HttpStatus.NOT_FOUND.value() == response.status()) {
      return new NotFoundException(requestUrl);
    }
    return errorStatus(methodKey, response);
  }

}
