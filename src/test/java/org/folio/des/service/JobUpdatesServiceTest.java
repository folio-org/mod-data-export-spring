package org.folio.des.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.config.KafkaConfiguration;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.entity.Job;
import org.folio.des.repository.JobRepository;
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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:job.sql")
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:clearDb.sql")
class JobUpdatesServiceTest extends BaseTest {

  @Autowired
  private JobUpdatesService updatesService;
  @Autowired
  private JobRepository repository;
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

    updatesService.onMessage(
        new ConsumerRecord<>(KafkaConfiguration.DATA_EXPORT_JOB_UPDATE_TOPIC_NAME, 0, 0, job.getId().toString(), job), () -> {
        });

    final Job savedJob = repository.findById(id).get();
    Assertions.assertAll(() -> assertEquals(job.getBatchStatus(), savedJob.getBatchStatus()),
        () -> assertEquals(job.getExitStatus(), savedJob.getExitStatus()),
        () -> assertEquals(job.getDescription(), savedJob.getDescription()),
        () -> assertEquals(job.getStatus(), savedJob.getStatus()), () -> assertEquals(job.getStartTime(), savedJob.getStartTime()),
        () -> assertEquals(job.getEndTime(), savedJob.getEndTime()), () -> assertEquals(job.getFiles(), savedJob.getFiles()),
        () -> assertEquals(job.getErrorDetails(), savedJob.getErrorDetails()));

  }

}
