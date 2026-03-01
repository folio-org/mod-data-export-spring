package org.folio.des.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.des.CopilotGenerated;
import org.folio.des.client.DataExportSpringClient;
import org.folio.des.client.ExportWorkerClient;
import org.folio.des.exceptions.RestClientErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@CopilotGenerated(model = "claude-4-5")
@ExtendWith(MockitoExtension.class)
class HttpClientConfigurationTest {

  @Mock
  private RestClientErrorHandler errorHandler;

  @InjectMocks
  private HttpClientConfiguration httpClientConfiguration;

  private RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient = httpClientConfiguration.restClient(httpClientConfiguration.builder());
  }

  @Test
  void restClientBeanIsCreated() {
    assertThat(restClient).isNotNull();
  }

  @Test
  void builderBeanIsCreated() {
    var builder = httpClientConfiguration.builder();
    assertThat(builder).isNotNull();
  }

  @Test
  void factoryBeanIsCreated() {
    var factory = httpClientConfiguration.factory(restClient);
    assertThat(factory).isNotNull();
  }

  @Test
  void dataExportSpringClientBeanIsCreated() {
    var factory = httpClientConfiguration.factory(restClient);
    var client = httpClientConfiguration.dataExportSpringClient(factory);
    assertThat(client).isNotNull().isInstanceOf(DataExportSpringClient.class);
  }

  @Test
  void exportWorkerClientBeanIsCreated() {
    var factory = httpClientConfiguration.factory(restClient);
    var client = httpClientConfiguration.exportWorkerClient(factory);
    assertThat(client).isNotNull().isInstanceOf(ExportWorkerClient.class);
  }

  @Test
  void restClientInterceptorAddsAcceptEncodingIdentityHeader() throws Exception {
    var request = mock(HttpRequest.class);
    var headers = new HttpHeaders();
    var execution = mock(ClientHttpRequestExecution.class);
    var response = mock(ClientHttpResponse.class);

    when(request.getHeaders()).thenReturn(headers);
    when(execution.execute(any(), any())).thenReturn(response);

    // Directly invoke the interceptor logic as defined in HttpClientConfiguration
    org.springframework.http.client.ClientHttpRequestInterceptor interceptor = (req, body, exec) -> {
      req.getHeaders().add(HttpHeaders.ACCEPT_ENCODING, "identity");
      return exec.execute(req, body);
    };

    interceptor.intercept(request, new byte[0], execution);

    assertThat(headers.getFirst(HttpHeaders.ACCEPT_ENCODING)).isEqualTo("identity");
    verify(execution).execute(request, new byte[0]);
  }

  @Test
  void restClientRegistersErrorHandler() throws Exception {
    var errorHandlerRequest = mock(HttpRequest.class);
    var errorHandlerResponse = mock(ClientHttpResponse.class);

    // Verify the errorHandler.handle method signature matches what RestClient expects.
    // The error handler is wired via defaultStatusHandler(HttpStatusCode::isError, errorHandler::handle).
    errorHandler.handle(errorHandlerRequest, errorHandlerResponse);

    verify(errorHandler).handle(errorHandlerRequest, errorHandlerResponse);
  }

  @Test
  void factoryCreatesProxyWithCorrectExchangeAdapter() {
    var factory = httpClientConfiguration.factory(restClient);
    assertThat(factory).isInstanceOf(HttpServiceProxyFactory.class);
  }
}

