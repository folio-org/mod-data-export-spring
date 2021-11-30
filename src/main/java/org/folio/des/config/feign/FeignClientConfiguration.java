package org.folio.des.config.feign;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class FeignClientConfiguration {
  @Bean
  public ErrorDecoder errorDecoder() {
    return new CustomFeignErrorDecoder();
  }
}
