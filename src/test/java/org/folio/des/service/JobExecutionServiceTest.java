package org.folio.des.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import org.folio.de.entity.Job;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTest {

  @Mock
  private JobCommandBuilderResolver jobCommandBuilderResolver;
  @Mock
  private ExportConfigValidatorResolver exportConfigValidatorResolver;
  @InjectMocks
  private JobExecutionService jobExecutionService;

  @Test
  void shouldPrepareStartJobCommandWithNoJobCommandBuilder() {
    var job = new Job();
    job.setType(ExportType.FAILED_LINKED_BIB_UPDATES);

    var command = jobExecutionService.prepareStartJobCommand(job);

    assertEquals(new HashMap<>(), command.getJobParameters().parameters());
  }
}
