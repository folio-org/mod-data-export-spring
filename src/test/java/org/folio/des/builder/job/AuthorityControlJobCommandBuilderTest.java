package org.folio.des.builder.job;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.folio.de.entity.Job;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthorityControlJobCommandBuilderTest {
  @Mock
  ObjectMapper objectMapper;
  @InjectMocks
  AuthorityControlJobCommandBuilder authorityControlJobCommandBuilder;

  @SuppressWarnings("deprecation")
  @Test
  void shouldHandleJsonProcessingException() throws JacksonException {
    var params = new ExportTypeSpecificParameters();
    var job = new Job();
    job.setExportTypeSpecificParameters(params);

    doThrow(JacksonException.class).when(objectMapper).writeValueAsString(any());

    assertThrows(IllegalArgumentException.class, () -> authorityControlJobCommandBuilder.buildJobCommand(job));
  }
}
