package org.folio.des.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.des.domain.dto.JobStatus;
import org.folio.de.entity.Job;
import org.folio.des.domain.dto.Progress;
import org.folio.des.repository.JobDataExportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobUpdatesServiceTest  {

  @Mock
  private JobDataExportRepository repository;
  @InjectMocks
  private JobUpdatesService updatesService;

  @Test
  @DisplayName("Update job")
  void updateJob() {
    var id = UUID.fromString("9d72fb44-eef7-4b9c-9bd9-f191feec6255");
    Job job = new Job();
    job.setId(id);
    job.setBatchStatus(BatchStatus.COMPLETED);
    job.setStatus(JobStatus.SUCCESSFUL);
    job.setDescription("Test job");
    job.setFiles(List.of("new test files"));
    job.setFileNames(List.of("new_file.json"));
    job.setStartTime(new Date());
    job.setEndTime(new Date());
    job.setErrorDetails("No errors");
    job.setExitStatus(ExitStatus.COMPLETED);
    job.setProgress(new Progress().progress(100).processed(1).total(1));

    var updatedJob = new Job();
    updatedJob.setId(id);
    updatedJob.setBatchStatus(BatchStatus.COMPLETED);
    updatedJob.setStatus(JobStatus.SUCCESSFUL);
    updatedJob.setDescription("Test job updated");
    updatedJob.setName("Updated job");
    updatedJob.setFiles(List.of("new test files"));
    updatedJob.setFileNames(List.of("new_file.json"));
    updatedJob.setStartTime(new Date());
    updatedJob.setEndTime(new Date());
    updatedJob.setErrorDetails("No errors");
    updatedJob.setExitStatus(ExitStatus.COMPLETED);
    updatedJob.setProgress(new Progress().progress(100).processed(1).total(1));

    doReturn(Optional.of(job)).when(repository).findById(id);

    updatesService.receiveJobExecutionUpdate(updatedJob);

    verify(repository).save(isA(Job.class));
  }

}
