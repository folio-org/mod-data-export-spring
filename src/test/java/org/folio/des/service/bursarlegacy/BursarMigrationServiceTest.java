package org.folio.des.service.bursarlegacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.des.domain.dto.BursarExportFilter;
import org.folio.des.domain.dto.BursarExportFilterAge;
import org.folio.des.domain.dto.BursarExportFilterCondition;
import org.folio.des.domain.dto.BursarExportFilterFeeFineOwner;
import org.folio.des.domain.dto.BursarExportFilterFeeType;
import org.folio.des.domain.dto.BursarExportFilterPatronGroup;
import org.folio.des.domain.dto.BursarExportTokenConditional;
import org.folio.des.domain.dto.BursarExportTokenConstant;
import org.folio.des.domain.dto.ExportTypeSpecificParametersWithLegacyBursar;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMapping;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMappings;
import org.folio.des.service.JobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BursarMigrationServiceTest {

  @InjectMocks
  private BursarMigrationService bursarMigrationService;

  @Mock
  private BursarExportLegacyJobService bursarExportLegacyJobService;

  @Mock
  private JobService jobService;

  @Test
  void testNoLegacyJobs() {
    when(bursarExportLegacyJobService.getAllLegacyJobs()).thenReturn(List.of());

    bursarMigrationService.updateLegacyBursarJobs(
      bursarExportLegacyJobService,
      jobService
    );

    verifyNoInteractions(jobService);
  }

  @Test
  void testLegacyJobConversion() {
    JobWithLegacyBursarParameters legacyJob = new JobWithLegacyBursarParameters();
    legacyJob.setId(UUID.fromString("928f4cc0-0f44-5d5f-aa73-3b7bc532961e"));
    legacyJob.setExportTypeSpecificParameters(
      new ExportTypeSpecificParametersWithLegacyBursar()
        .bursarFeeFines(
          new LegacyBursarFeeFines()
            .daysOutstanding(1)
            .addPatronGroupsItem("ed98f8c2-09b0-5d46-b610-ba955d0bf303")
            .addPatronGroupsItem("d82b6807-4ab3-5412-8428-1b49ac20e0c2")
        )
    );

    // check converted job makes sense
    Job job = BursarMigrationService.convertLegacyJob(legacyJob);
    assertEquals(
      "928f4cc0-0f44-5d5f-aa73-3b7bc532961e",
      job.getId().toString()
    );

    // verify filters
    List<BursarExportFilter> filters =
      (
        (BursarExportFilterCondition) job
          .getExportTypeSpecificParameters()
          .getBursarFeeFines()
          .getFilter()
      ).getCriteria();

      // age filter
    assertEquals(
      1,
      filters
        .stream()
        .filter(i -> i instanceof BursarExportFilterAge)
        .map(i -> (BursarExportFilterAge) i)
        .findFirst()
        .get()
        .getNumDays()
    );

    // patron filters are nested inside an OR
    List<BursarExportFilterPatronGroup> patronFilters = filters
      .stream()
      .filter(i -> i instanceof BursarExportTokenConditional)
      .map(i -> (BursarExportTokenConditional) i)
      .findFirst()
      .get()
      .getConditions()
      .stream()
      .map(i -> (BursarExportFilterPatronGroup) i.getCondition())
      .toList();
    assertEquals(2, patronFilters.size());
    assertTrue(
      patronFilters
        .stream()
        .map(BursarExportFilterPatronGroup::getPatronGroupId)
        .anyMatch(i -> i.equals(UUID.fromString("ed98f8c2-09b0-5d46-b610-ba955d0bf303")))
    );
    assertTrue(
      patronFilters
        .stream()
        .map(BursarExportFilterPatronGroup::getPatronGroupId)
        .anyMatch(i -> i.equals(UUID.fromString("d82b6807-4ab3-5412-8428-1b49ac20e0c2")))
    );

    // ensure that calling .updateLegacyBursarJobs correctly updates the job above
    when(bursarExportLegacyJobService.getAllLegacyJobs())
      .thenReturn(List.of(legacyJob));

    bursarMigrationService.updateLegacyBursarJobs(
      bursarExportLegacyJobService,
      jobService
    );

    verify(bursarExportLegacyJobService, times(1)).getAllLegacyJobs();
    verify(jobService, times(1)).upsertAndSendToKafka(job, false);
  }

  @Test
  void testMapTypeMappingsToTokens() {
    LegacyBursarFeeFinesTypeMappings typeMappings = new LegacyBursarFeeFinesTypeMappings();
    LegacyBursarFeeFinesTypeMapping typeMapping = new LegacyBursarFeeFinesTypeMapping();
    typeMapping.setFeefineTypeId(UUID.fromString("c4ff3edb-2cc4-523c-a90d-9a2fc8b02a00"));
    typeMapping.setItemType("test_item_type");
    typeMapping.setItemDescription("test_item_description");
    typeMappings.put(
      "81a5bb0a-e3a1-57b5-b8a8-9324989a7585",
      List.of(typeMapping)
    );

    List<BursarExportTokenConditional> tokens = BursarMigrationService.mapTypeMappingsToTokens(typeMappings);

    // Type checking for item token
    assertEquals(
      BursarExportFilterCondition.class,
      tokens.get(0).getConditions().get(0).getCondition().getClass()
    );

    // Check for item token condition
    BursarExportFilterCondition itemTypeCondition = (BursarExportFilterCondition) tokens
      .get(0)
      .getConditions()
      .get(0)
      .getCondition();
    assertEquals(
      BursarExportFilterFeeType.class,
      itemTypeCondition.getCriteria().get(0).getClass()
    );
    BursarExportFilterFeeType feeType = (BursarExportFilterFeeType) itemTypeCondition
      .getCriteria()
      .get(0);
    assertEquals(
      UUID.fromString("c4ff3edb-2cc4-523c-a90d-9a2fc8b02a00"),
      feeType.getFeeFineTypeId()
    );
    assertEquals(
      BursarExportFilterFeeFineOwner.class,
      itemTypeCondition.getCriteria().get(1).getClass()
    );
    BursarExportFilterFeeFineOwner feeFineOwner = (BursarExportFilterFeeFineOwner) itemTypeCondition
      .getCriteria()
      .get(1);
    assertEquals(
      UUID.fromString("81a5bb0a-e3a1-57b5-b8a8-9324989a7585"),
      feeFineOwner.getFeeFineOwner()
    );
    assertEquals(
      BursarExportTokenConstant.class,
      tokens.get(0).getConditions().get(0).getValue().getClass()
    );
    assertEquals(
      "test_item_type",
      (
        (BursarExportTokenConstant) tokens
          .get(0)
          .getConditions()
          .get(0)
          .getValue()
      ).getValue()
    );

    // Type checking for description token
    assertEquals(
      BursarExportFilterCondition.class,
      tokens.get(1).getConditions().get(0).getCondition().getClass()
    );
    assertEquals(
      BursarExportTokenConstant.class,
      tokens.get(1).getConditions().get(0).getValue().getClass()
    );
    assertEquals(
      "test_item_description",
      (
        (BursarExportTokenConstant) tokens
          .get(1)
          .getConditions()
          .get(0)
          .getValue()
      ).getValue()
    );
  }
}
