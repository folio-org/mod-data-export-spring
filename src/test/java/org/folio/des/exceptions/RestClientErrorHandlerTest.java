package org.folio.des.exceptions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.folio.des.CopilotGenerated;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

@CopilotGenerated(model = "claude-4-5")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RestClientErrorHandlerTest {

  @Mock
  private HttpRequest request;
  @Mock
  private ClientHttpResponse response;
  @InjectMocks
  private RestClientErrorHandler restClientErrorHandler;

  @Test
  void shouldThrowNotFoundExceptionWhenStatusIs404() throws IOException {
    when(request.getURI()).thenReturn(URI.create("http://localhost/test"));
    when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

    assertThatThrownBy(() -> restClientErrorHandler.handle(request, response))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Not found: http://localhost/test");
  }

  @Test
  void shouldThrowRuntimeExceptionWithBodyMessageWhenStatusIsNot404() throws IOException {
    var errorBody = "Internal Server Error details";

    when(request.getURI()).thenReturn(URI.create("http://localhost/resource"));
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(errorBody.getBytes()));

    assertThatThrownBy(() -> restClientErrorHandler.handle(request, response))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("http://localhost/resource")
        .hasMessageContaining(errorBody);
  }

  @Test
  void shouldThrowRuntimeExceptionWithUnknownErrorWhenBodyIsBlank() throws IOException {
    when(request.getURI()).thenReturn(URI.create("http://localhost/resource"));
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenReturn(new ByteArrayInputStream("   ".getBytes()));

    assertThatThrownBy(() -> restClientErrorHandler.handle(request, response))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("http://localhost/resource")
        .hasMessageContaining("Unknown error");
  }

  @Test
  void shouldThrowRuntimeExceptionWhenBodyThrowsIOException() throws IOException {
    when(request.getURI()).thenReturn(URI.create("http://localhost/resource"));
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenThrow(new IOException("stream error"));

    assertThatThrownBy(() -> restClientErrorHandler.handle(request, response))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Unable to get reason for error: stream error");
  }
}
