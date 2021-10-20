package org.folio.des.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:job.sql")
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:clearDb.sql")
class JobUpdatesServiceTest extends BaseTest {

  @Autowired
  private JobUpdatesService updatesService;
  @Autowired
  private JobDataExportRepository repository;
  @Autowired
  private FolioExecutionContextHelper contextHelper;

  @BeforeEach
  void setUp() {
    contextHelper.initScope();
  }

  @Test
  @DisplayName("Update job")
  void updateJob() {
    Job job = new Job();
    var id = UUID.fromString("9d72fb44-eef7-4b9c-9bd9-f191feec6255");
    job.setId(id);
    job.setBatchStatus(BatchStatus.COMPLETED);
    job.setStatus(JobStatus.SUCCESSFUL);
    job.setDescription("Test job updated");
    job.setFiles(List.of("new test files"));
    job.setStartTime(new Date());
    job.setEndTime(new Date());
    job.setErrorDetails("No errors");
    job.setExitStatus(ExitStatus.COMPLETED);

    updatesService.receiveJobExecutionUpdate(job);

    final Job savedJob = repository.findById(id).orElse(null);
    Assertions.assertAll(() -> Assertions.assertNotNull(savedJob),
        () -> Assertions.assertEquals(job.getBatchStatus(), savedJob.getBatchStatus()),
        () -> Assertions.assertEquals(job.getExitStatus(), savedJob.getExitStatus()),
        () -> Assertions.assertEquals(job.getDescription(), savedJob.getDescription()),
        () -> Assertions.assertEquals(job.getStatus(), savedJob.getStatus()),
        () -> Assertions.assertEquals(job.getStartTime(), savedJob.getStartTime()),
        () -> Assertions.assertEquals(job.getEndTime(), savedJob.getEndTime()),
        () -> Assertions.assertEquals(job.getFiles(), savedJob.getFiles()),
        () -> Assertions.assertEquals(job.getErrorDetails(), savedJob.getErrorDetails()));
  }

}
