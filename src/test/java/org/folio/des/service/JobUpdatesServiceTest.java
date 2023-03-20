package org.folio.des.service;

import static org.folio.des.support.BaseTest.TENANT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.de.entity.Job;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.domain.dto.Progress;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

@ExtendWith(MockitoExtension.class)
class JobUpdatesServiceTest {

  @Mock
  private JobDataExportRepository repository;
  @InjectMocks
  private JobUpdatesService updatesService;
  private JobUpdatesListenerService updatesListenerService;

  protected Map<String, Object> okapiHeaders;

  @BeforeEach
  void init() {
    updatesListenerService = new JobUpdatesListenerService(new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return "mod-data-export-spring";
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return tenantId + "_mod_data_export_spring";
      }
    }, updatesService);
  }

  @BeforeEach
  void setUp() {
    okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, TENANT);
    okapiHeaders.put(XOkapiHeaders.TOKEN, "TOKEN");
    okapiHeaders.put(XOkapiHeaders.URL, "URL");
    okapiHeaders.put(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());
  }

  @Test
  @DisplayName("Update job with change")
  void updateJobWithChange() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();

    var id = UUID.randomUUID();
    Job job = new Job();
    job.setId(id);
    job.setBatchStatus(BatchStatus.COMPLETED);
    job.setStatus(JobStatus.SUCCESSFUL);
    job.setDescription("Test job");
    job.setExportTypeSpecificParameters(parameters);
    job.setProgress(new Progress().progress(100).processed(1).total(1));

    config.setConfigName("testConfig");
    parameters.setVendorEdiOrdersExportConfig(config);

    var updatedJob = new Job();
    updatedJob.setId(id);
    updatedJob.setDescription("Test job updated");
    updatedJob.setFiles(List.of("new test files"));
    updatedJob.setFileNames(List.of("new_file.json"));
    updatedJob.setBatchStatus(BatchStatus.COMPLETED);
    updatedJob.setStatus(JobStatus.SUCCESSFUL);
    updatedJob.setName("Updated job");
    updatedJob.setStartTime(new Date());
    updatedJob.setEndTime(new Date());
    updatedJob.setExitStatus(ExitStatus.COMPLETED);
    updatedJob.setExportTypeSpecificParameters(parameters);
    updatedJob.setProgress(new Progress().progress(100).processed(1).total(1));

    doReturn(Optional.of(job)).when(repository).findById(id);
    doReturn(job).when(repository).save(any());

    updatesListenerService.receiveJobExecutionUpdate(updatedJob, okapiHeaders);

    verify(repository, times(1)).save(any());
  }

  @Test
  @DisplayName("Update job with error")
  void updateJobWithError() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();

    var id = UUID.randomUUID();
    Job job = new Job();
    job.setId(id);
    job.setBatchStatus(BatchStatus.COMPLETED);
    job.setStatus(JobStatus.SUCCESSFUL);
    job.setDescription("Test job");
    job.setStartTime(new Date());
    job.setEndTime(new Date());
    job.setExportTypeSpecificParameters(parameters);
    job.setProgress(new Progress().progress(100).processed(1).total(1));

    config.setConfigName("testConfig");
    parameters.setVendorEdiOrdersExportConfig(config);

    var updatedJob = new Job();
    updatedJob.setId(id);
    updatedJob.setDescription("Test job updated");
    updatedJob.setErrorDetails("Something went wrong");

    doReturn(Optional.of(job)).when(repository).findById(id);
    doReturn(job).when(repository).save(any());

    updatesListenerService.receiveJobExecutionUpdate(updatedJob, okapiHeaders);

    verify(repository, times(1)).save(any());
  }

  @Test
  @DisplayName("Update job without change")
  void updateJobWithoutChange() {
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
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
    job.setExportTypeSpecificParameters(parameters);
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
    job.setExportTypeSpecificParameters(parameters);
    updatedJob.setProgress(new Progress().progress(100).processed(1).total(1));

    doReturn(Optional.of(job)).when(repository).findById(id);

    updatesListenerService.receiveJobExecutionUpdate(updatedJob, okapiHeaders);

    verify(repository).save(isA(Job.class));
  }

  @Test
  @DisplayName("Failed job")
  void failedJob() {
    var id = UUID.randomUUID();
    var updatedJob = new Job();
    updatedJob.setId(id);

    doReturn(Optional.empty()).when(repository).findById(id);

    updatesListenerService.receiveJobExecutionUpdate(updatedJob, okapiHeaders);

    verify(repository, never()).save(any());
  }
}
