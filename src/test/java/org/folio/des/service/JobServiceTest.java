package org.folio.des.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.de.entity.JobCommand;
import org.folio.des.builder.job.JobCommandBuilderResolver;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.converter.DefaultModelConfigToExportConfigConverter;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.de.entity.Job;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.des.service.impl.JobServiceImpl;
import org.folio.des.validator.ExportConfigValidatorResolver;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest
class JobServiceTest {
  private static final int DEFAULT_JOB_EXPIRATION_PERIOD = 7;
  private static final int DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD = 14;

  @Mock
  private JobServiceImpl jobService;
  @Mock
  private JobDataExportRepository repository;
  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private BulkEditConfigService configService;
  @Mock
  private ConfigurationClient client;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private JobCommandBuilderResolver jobCommandBuilderResolver;
  @Mock
  private ExportConfigValidatorResolver exportConfigValidatorResolver;
  @Mock
  private DefaultModelConfigToExportConfigConverter defaultModelConfigToExportConfigConverter;
  @Mock
  private KafkaService kafka;
  @Mock
  private ObjectMapper objectMapper;


  @Test
  void shouldCollectExpiredJobs() {
    var expiredJobs = createExpiredJobs();

    var jobsExpirationDate = getExpiredDate(DEFAULT_JOB_EXPIRATION_PERIOD);
    var bulkEditExpirationDate = getExpiredDate(DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD);

    Mockito.when(repository.findByUpdatedDateBefore(any())).thenReturn(expiredJobs);
    Mockito.when(configService.getBulkEditJobExpirationPeriod()).thenReturn(DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD);

    jobService.deleteOldJobs();

    verify(repository).findByUpdatedDateBefore(jobsExpirationDate);
    verify(repository).findByUpdatedDateBefore(bulkEditExpirationDate);
    verify(jobExecutionService).deleteJobs(expiredJobs);
  }

  @Test
  void testResendJob() {
    var id = UUID.randomUUID();
    var repository = mock(JobDataExportRepository.class);
    Job job = new Job();
    job.setId(UUID.randomUUID());
    when(repository.save(any(Job.class))).thenReturn(job);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    var folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    var jobExecutionService = new JobExecutionService(kafka, exportConfigValidatorResolver, jobCommandBuilderResolver, defaultModelConfigToExportConfigConverter, client);
    var jobService = new JobServiceImpl(jobExecutionService, repository, folioExecutionContext, null, null, client);
    var config = new ExportConfig();
    config.setId(id.toString());
    org.folio.des.domain.dto.Job jobDto = new org.folio.des.domain.dto.Job();
    ArrayList<String> list = new ArrayList<>();
    list.add("TestFile");
    job.setFileNames(list);
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    vendorEdiOrdersExportConfig.setExportConfigId(id);
    vendorEdiOrdersExportConfig.setConfigName("Test");
    EdiFtp ediFtp = new EdiFtp();
    ediFtp.setFtpConnMode(EdiFtp.FtpConnModeEnum.PASSIVE);
    ediFtp.setFtpFormat(EdiFtp.FtpFormatEnum.FTP);
    ediFtp.setPassword("password");
    vendorEdiOrdersExportConfig.setEdiFtp(ediFtp);
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    exportTypeSpecificParameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    config.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    job.setExportTypeSpecificParameters(exportTypeSpecificParameters);
    job.setFileNames(list);
    job.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    jobDto.setExportTypeSpecificParameters(exportTypeSpecificParameters);

    when(defaultModelConfigToExportConfigConverter.convert(any())).then(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        ExportConfig exportConfig = new ExportConfig();
        exportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
        exportConfig.setExportTypeSpecificParameters(exportTypeSpecificParameters);

        return exportConfig;
      }
    });
    jobService.resend(jobDto);
    JobCommand command = jobExecutionService.prepareResendJobCommand(job);
    assertEquals("TestFile",command.getJobParameters().getParameters().get("fileName").toString());

  }

  private List<Job> createExpiredJobs() {
    var bulkEditExportTypes = new ArrayList<Job>();
    var exportTypes = new ArrayList<Job>();

    Stream.of(ExportType.values())
      .forEach(exportType -> {
        if (isBulkEdit(exportType)) {
          bulkEditExportTypes.add(createJob(exportType, DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD));
        } else {
          exportTypes.add(createJob(exportType, DEFAULT_JOB_EXPIRATION_PERIOD));
        }
      });

    exportTypes.addAll(bulkEditExportTypes);
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

  private boolean isBulkEdit(ExportType exportType) {
    return exportType.getValue().startsWith("BULK_EDIT");
  }
}
