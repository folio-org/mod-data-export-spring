package org.folio.des.service.bursarlegacy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.de.entity.bursarlegacy.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.repository.bursarlegacy.BursarExportLegacyJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BursarExportLegacyJobServiceTest {

  @Mock
  private BursarExportLegacyJobRepository repository;

  static JobWithLegacyBursarParameters nonBursar1, nonBursar2, nonBursar3, isBursar;

  static {
    nonBursar1 = new JobWithLegacyBursarParameters();
    nonBursar1.setId(UUID.fromString("998a84d7-9538-5652-8a40-d26bbb8507bc"));
    nonBursar1.setExportTypeSpecificParameters(new ExportTypeSpecificParametersWithLegacyBursar());

    nonBursar2 = new JobWithLegacyBursarParameters();
    nonBursar2.setId(UUID.fromString("dfad2baa-c970-5933-b86a-68781c5c4f2c"));
    nonBursar2.setExportTypeSpecificParameters(new ExportTypeSpecificParametersWithLegacyBursar().bursarFeeFines(null));

    nonBursar3 = new JobWithLegacyBursarParameters();
    nonBursar3.setId(UUID.fromString("aae30cd1-4549-5572-bb14-4c04c1b28366"));
    nonBursar3.setExportTypeSpecificParameters(
        new ExportTypeSpecificParametersWithLegacyBursar().bursarFeeFines(new LegacyBursarFeeFines()));

    isBursar = new JobWithLegacyBursarParameters();
    isBursar.setId(UUID.fromString("5a0e5c0b-a216-552c-8a28-6b25ada9ae84"));
    isBursar.setExportTypeSpecificParameters(
        new ExportTypeSpecificParametersWithLegacyBursar().bursarFeeFines(new LegacyBursarFeeFines().daysOutstanding(1)));
  }

  @Test
  void testGetBlankQuery() {
    List<JobWithLegacyBursarParameters> items = List.of(nonBursar1, nonBursar2, nonBursar3, isBursar);

    when(repository.findAll()).thenReturn(items);

    BursarExportLegacyJobService service = new BursarExportLegacyJobService(repository);

    List<org.folio.des.domain.dto.JobWithLegacyBursarParameters> legacyJobCollection = service.getAllLegacyJobs();

    assertEquals(1, legacyJobCollection.size());
    assertEquals(isBursar.getId(), legacyJobCollection.get(0)
      .getId());
  }

  private static List<Arguments> hasLegacyParametersArguments() {
    return List.of(Arguments.of(isBursar, true), Arguments.of(nonBursar1, false), Arguments.of(nonBursar2, false),
        Arguments.of(nonBursar3, false));
  }

  @ParameterizedTest
  @MethodSource("hasLegacyParametersArguments")
  void testHasLegacyBursarParameters(JobWithLegacyBursarParameters job, boolean expected) {
    assertEquals(expected, BursarExportLegacyJobService.hasLegacyBursarParameters(job));
  }
}
