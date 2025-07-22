package org.folio.des.service;

import org.folio.de.entity.JobDeletionIntervalEntity;
import org.folio.des.CopilotGenerated;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.delete_interval.JobDeletionInterval;
import org.folio.des.repository.JobDeletionIntervalRepository;
import org.folio.des.service.impl.JobDeletionIntervalServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@CopilotGenerated(model = "Claude Sonnet 3.5")
class JobDeletionIntervalServiceTest {
  @Mock
  private JobDeletionIntervalRepository repository;

  @Mock
  private FolioExecutionContext context;

  @InjectMocks
  private JobDeletionIntervalServiceImpl service;

  @Test
  void getAllReturnsAllIntervals() {
    var entity = new JobDeletionIntervalEntity();
    entity.setExportType(ExportType.BURSAR_FEES_FINES);
    entity.setRetentionDays(10);

    when(repository.findAll()).thenReturn(List.of(entity));

    var result = service.getAll();

    assertEquals(1, result.getTotalRecords());
    assertEquals(ExportType.BURSAR_FEES_FINES, result.getJobDeletionIntervals().get(0).getExportType());
    assertEquals(10, result.getJobDeletionIntervals().get(0).getRetentionDays());
  }

  @Test
  void getThrowsNotFoundExceptionWhenIntervalDoesNotExist() {
    when(repository.findById(ExportType.BURSAR_FEES_FINES)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.get(ExportType.BURSAR_FEES_FINES));
  }

  @Test
  void createThrowsConflictWhenIntervalAlreadyExists() {
    var interval = new JobDeletionInterval();
    interval.setExportType(ExportType.BURSAR_FEES_FINES);

    when(repository.existsById(ExportType.BURSAR_FEES_FINES)).thenReturn(true);

    assertThrows(ResponseStatusException.class, () -> service.create(interval));
  }

  @Test
  void updateThrowsNotFoundWhenIntervalDoesNotExist() {
    var interval = new JobDeletionInterval();
    interval.setExportType(ExportType.BURSAR_FEES_FINES);

    when(repository.existsById(ExportType.BURSAR_FEES_FINES)).thenReturn(false);

    assertThrows(ResponseStatusException.class, () -> service.update(interval));
  }

  @Test
  void deleteRemovesIntervalWhenExists() {
    doNothing().when(repository).deleteById(ExportType.BURSAR_FEES_FINES);

    service.delete(ExportType.BURSAR_FEES_FINES);

    verify(repository, times(1)).deleteById(ExportType.BURSAR_FEES_FINES);
  }
}
