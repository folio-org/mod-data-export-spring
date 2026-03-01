package org.folio.des.service;

import static org.folio.des.domain.dto.ExportType.BURSAR_FEES_FINES;
import static org.folio.des.domain.dto.ExportType.CLAIMS;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.folio.de.entity.Job;
import org.folio.de.entity.JobCommand;
import org.folio.des.CopilotGenerated;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.client.ExportWorkerClient;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.domain.dto.delete_interval.JobDeletionInterval;
import org.folio.des.domain.dto.delete_interval.JobDeletionIntervalCollection;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.service.config.impl.BaseExportConfigService;
import org.folio.des.service.impl.JobServiceImpl;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

  @InjectMocks
  private JobServiceImpl jobService;
  @Mock
  private JobDataExportRepository repository;
  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private ExportWorkerClient exportWorkerClient;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private JobCommandBuilderResolver jobCommandBuilderResolver;
  @Mock
  private ExportConfigValidatorResolver exportConfigValidatorResolver;
  @Mock
  private BaseExportConfigService defaultExportConfigService;
  @Mock
  private KafkaService kafka;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private JobDeletionIntervalService deletionIntervalService;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(jobService, "jobExpirationPeriod", 7);
    ReflectionTestUtils.setField(jobService, "jobDownloadFileConnectionTimeout", 5000);
  }

  @Test
  @SneakyThrows
  void testResendJob() {
    UUID configId = UUID.randomUUID();
    Job job = new Job();
    job.setId(UUID.randomUUID());
    ArrayList<String> list = new ArrayList<>();
    list.add("TestFile.csv");
    job.setFiles(new ArrayList<>());
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setExportConfigId(configId);
    vendorEdiOrdersExportConfig.setConfigName("Test");
    EdiFtp ediFtp = new EdiFtp();
    ediFtp.setFtpConnMode(EdiFtp.FtpConnModeEnum.PASSIVE);
    ediFtp.setFtpFormat(EdiFtp.FtpFormatEnum.FTP);
    ediFtp.setPassword("password");
    vendorEdiOrdersExportConfig.setEdiFtp(ediFtp);
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    exportTypeSpecificParameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    job.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    when(repository.findById(any())).thenReturn(Optional.of(job));
    when(objectMapper.writeValueAsString(any())).thenReturn("any non-null string");
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    var folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    var jobExecutionService = new JobExecutionService(kafka, exportConfigValidatorResolver, jobCommandBuilderResolver, defaultExportConfigService, objectMapper);
    var internalJobService = new JobServiceImpl(exportWorkerClient, jobExecutionService, repository, folioExecutionContext, null, deletionIntervalService, defaultExportConfigService);
    var config = new ExportConfig();
    config.setId(configId.toString());
    org.folio.des.domain.dto.Job jobDto = new org.folio.des.domain.dto.Job();
    config.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    jobDto.setId(UUID.randomUUID());
    jobDto.setExportTypeSpecificParameters(exportTypeSpecificParameters);

    when(defaultExportConfigService.getConfigById(any())).then(invocationOnMock ->
      new ExportConfig().type(ExportType.EDIFACT_ORDERS_EXPORT)
        .exportTypeSpecificParameters(exportTypeSpecificParameters));
    when(repository.findById(any())).thenReturn(java.util.Optional.of(job));
    assertThrows(NotFoundException.class, () -> internalJobService.resendExportedFile(configId));
    job.setFileNames(list);
    internalJobService.resendExportedFile(jobDto.getId());
    JobCommand command = jobExecutionService.prepareResendJobCommand(job);
    Assertions.assertEquals("TestFile.csv", command.getJobParameters().getParameter("FILE_NAME").value());
    Assertions.assertNotNull(command.getJobParameters().getParameter("EDIFACT_ORDERS_EXPORT"));
  }

  @Test
  @CopilotGenerated(model = "Claude Sonnet 3.5")
  void shouldDeleteExpiredJobsWhenIntervalsExist() {
    var bursarExpiredJobs = createExpiredJobs(5, BURSAR_FEES_FINES, 7);
    var claimsExpiredJobs = createExpiredJobs(10, CLAIMS, 14);
    var date7Days = getExpiredDate(7);
    var date14Days = getExpiredDate(14);

    var interval1 = new JobDeletionInterval()
      .exportType(BURSAR_FEES_FINES)
      .retentionDays(7);
    var interval2 = new JobDeletionInterval()
      .exportType(ExportType.CLAIMS)
      .retentionDays(14);
    var jobDeletionIntervals = List.of(interval1, interval2);

    var jobDeletionIntervalCollection = new JobDeletionIntervalCollection()
      .jobDeletionIntervals(jobDeletionIntervals);

    when(deletionIntervalService.getAll()).thenReturn(jobDeletionIntervalCollection);
    when(repository.findByTypeAndUpdatedDateBefore(BURSAR_FEES_FINES, date7Days)).thenReturn(bursarExpiredJobs);
    when(repository.findByTypeAndUpdatedDateBefore(ExportType.CLAIMS, date14Days)).thenReturn(claimsExpiredJobs);

    var expiredJobs = new ArrayList<Job>();
    expiredJobs.addAll(bursarExpiredJobs);
    expiredJobs.addAll(claimsExpiredJobs);

    jobService.deleteOldJobs();

    verify(repository).findByTypeAndUpdatedDateBefore(BURSAR_FEES_FINES, date7Days);
    verify(repository).findByTypeAndUpdatedDateBefore(ExportType.CLAIMS, date14Days);
    verify(repository).deleteAllInBatch(expiredJobs);
    verify(jobExecutionService).deleteJobs(expiredJobs);
  }

  @Test
  @CopilotGenerated(model = "Claude Sonnet 3.5")
  void shouldNotDeleteJobsWhenTheyAreNotExpired() {
    var date200Days = getExpiredDate(200);

    var interval1 = new JobDeletionInterval()
      .exportType(BURSAR_FEES_FINES)
      .retentionDays(200); // 200 days retention period
    var jobDeletionIntervals = List.of(interval1);

    var jobDeletionIntervalCollection = new JobDeletionIntervalCollection()
      .jobDeletionIntervals(jobDeletionIntervals);

    when(deletionIntervalService.getAll()).thenReturn(jobDeletionIntervalCollection);
    when(repository.findByTypeAndUpdatedDateBefore(BURSAR_FEES_FINES, date200Days)).thenReturn(List.of());

    jobService.deleteOldJobs();

    verify(repository).findByTypeAndUpdatedDateBefore(BURSAR_FEES_FINES, date200Days);
    verify(repository, never()).deleteAllInBatch(any());
    verify(jobExecutionService, never()).deleteJobs(any());
  }

  @Test
  @CopilotGenerated(model = "Claude Sonnet 3.5")
  void shouldSkipDeletionWhenNoIntervalsExist() {
    var emptyCollection = new JobDeletionIntervalCollection()
      .jobDeletionIntervals(List.of());

    when(deletionIntervalService.getAll()).thenReturn(emptyCollection);

    jobService.deleteOldJobs();

    verify(repository, never()).findByTypeAndUpdatedDateBefore(any(), any());
    verify(repository, never()).deleteAllInBatch(any());
    verify(jobExecutionService, never()).deleteJobs(any());
  }

  private List<Job> createExpiredJobs(int count, ExportType exportType, int expirationPeriod) {
    var exportTypes = new ArrayList<Job>();

    for (int i = 0; i < count; i++) {
      exportTypes.add(createJob(exportType, expirationPeriod));
    }

    return exportTypes;
  }

  private Job createJob(ExportType exportType, int expirationPeriod) {
    var job = new Job();
    job.setId(UUID.randomUUID());
    job.setType(exportType);
    job.setUpdatedDate(getExpiredDate(expirationPeriod));
    return job;
  }

  private Date getExpiredDate(int days) {
    return Date.from(LocalDate.now().minusDays(days).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
  }
}
