package org.folio.des.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.folio.des.domain.dto.ExportType;
import org.folio.de.entity.Job;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.service.config.BulkEditConfigService;
import org.folio.des.service.impl.JobServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest
class JobServiceTest {
  private static final int DEFAULT_JOB_EXPIRATION_PERIOD = 7;
  private static final int DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD = 14;

  @InjectMocks
  private JobServiceImpl jobService;
  @Mock
  private JobDataExportRepository repository;
  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private BulkEditConfigService configService;

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
