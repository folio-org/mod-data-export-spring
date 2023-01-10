package org.folio.des.builder.job;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.de.entity.Job;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorityControlJobCommandBuilderTest {
  @Mock
  ObjectMapper objectMapper;
  @InjectMocks
  AuthorityControlJobCommandBuilder authorityControlJobCommandBuilder;

  @SuppressWarnings("deprecation")
  @Test
  void shouldHandleJsonProcessingException() throws JsonProcessingException {
    var params = new ExportTypeSpecificParameters();
    var job = new Job();
    job.setExportTypeSpecificParameters(params);

    doThrow(new JsonMappingException("")).when(objectMapper).writeValueAsString(any());

    assertThrows(IllegalArgumentException.class, () -> authorityControlJobCommandBuilder.buildJobCommand(job));
  }
}
