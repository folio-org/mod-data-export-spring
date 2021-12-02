package org.folio.des.config.feign;

import org.springframework.context.annotation.Bean;

import feign.codec.ErrorDecoder;

public class FeignClientConfiguration {
  @Bean
  public ErrorDecoder errorDecoder() {
    return new CustomFeignErrorDecoder();
  }
}
