package org.folio.des.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.client.DataExportSpringClient;
import org.folio.des.client.ExportWorkerClient;
import org.folio.des.exceptions.RestClientErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class HttpClientConfiguration {

  private final RestClientErrorHandler errorHandler;

  @Bean
  public DataExportSpringClient dataExportSpringClient(HttpServiceProxyFactory factory) {
    return factory.createClient(DataExportSpringClient.class);
  }

  @Bean
  public ExportWorkerClient exportWorkerClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ExportWorkerClient.class);
  }

  @Primary
  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder
        .requestInterceptor(
            (request, body, execution) -> {
              log.info("Request URL: {}", request.getURI());
              request.getHeaders().add(HttpHeaders.ACCEPT_ENCODING, "identity");
              return execution.execute(request, body);
            })
        .defaultStatusHandler(HttpStatusCode::isError, errorHandler::handle)
        .build();
  }

  @Bean
  public RestClient.Builder builder() {
    return RestClient.builder();
  }

  @Bean
  public HttpServiceProxyFactory factory(RestClient restClient) {
    return HttpServiceProxyFactory
          .builder()
          .exchangeAdapter(RestClientAdapter.create(restClient))
          .build();
  }
}
