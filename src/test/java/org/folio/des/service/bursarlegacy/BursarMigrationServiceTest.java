package org.folio.des.service.bursarlegacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.folio.des.domain.dto.JobWithLegacyBursarParametersCollection;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMapping;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMappings;
import org.folio.des.service.JobService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
  void testIsLegacyJob() {
    JobWithLegacyBursarParameters job = mockJob();

    JobWithLegacyBursarParameters jobWithLegacyBursarParameters = mockJob();
    jobWithLegacyBursarParameters
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setDaysOutstanding(1);

    Assertions.assertAll(
      () -> assertFalse(bursarMigrationService.isLegacyJob(job)),
      () -> assertTrue(bursarMigrationService.isLegacyJob(jobWithLegacyBursarParameters))
    );
  }

  @Test
  void testRecreateNoLegacyJob() {
    List<JobWithLegacyBursarParameters> jobsWithLegacyBursarParameters = new ArrayList<>();
    JobWithLegacyBursarParameters job = mockJob();
    job.setId(UUID.fromString("0000-00-00-00-000000"));
    jobsWithLegacyBursarParameters.add(job);

    Mockito
      .when(bursarExportLegacyJobService.get(0, 10000, "status==SCHEDULED"))
      .thenReturn(mockLegacyCollectionOneItem(jobsWithLegacyBursarParameters));

    bursarMigrationService.recreateLegacyJobs(
      bursarExportLegacyJobService,
      jobService
    );

    Mockito
      .verify(jobService, times(0))
      .upsertAndSendToKafka(any(Job.class), eq(true));
  }

  @Test
  void testRecreateOneLegacyJob() {
    List<JobWithLegacyBursarParameters> jobsWithLegacyBursarParameters = new ArrayList<>();
    JobWithLegacyBursarParameters jobWithLegacyBursarParameters = mockJob();
    jobWithLegacyBursarParameters.setId(UUID.fromString("0000-00-00-00-000000"));
    jobWithLegacyBursarParameters
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setDaysOutstanding(1);
    jobWithLegacyBursarParameters
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setPatronGroups(Arrays.asList("0000-00-00-00-000001"));
    jobsWithLegacyBursarParameters.add(jobWithLegacyBursarParameters);

    Job job = bursarMigrationService.prepareNewJob();
    job.setId(UUID.fromString("0000-00-00-00-000000"));
    BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
    filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);
    List<BursarExportFilter> filterConditions = new ArrayList<>();
    BursarExportFilterAge ageFilter = new BursarExportFilterAge();
    ageFilter.setNumDays(1);
    ageFilter.setCondition(
      BursarExportFilterAge.ConditionEnum.GREATER_THAN_EQUAL
    );
    BursarExportFilterCondition patronGroupListFilter = new BursarExportFilterCondition();
    patronGroupListFilter.setOperation(
      BursarExportFilterCondition.OperationEnum.OR
    );
    List<BursarExportFilter> patronGroupFilters = new ArrayList<>();
    BursarExportFilterPatronGroup patronGroupFilter = new BursarExportFilterPatronGroup();
    patronGroupFilter.setPatronGroupId(UUID.fromString("0000-00-00-00-000001"));
    patronGroupFilters.add(patronGroupFilter);
    patronGroupListFilter.setCriteria(patronGroupFilters);

    filterConditions.add(ageFilter);
    filterConditions.add(patronGroupListFilter);

    filterBase.setCriteria(filterConditions);

    job
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setFilter(filterBase);

    Mockito
      .when(bursarExportLegacyJobService.get(0, 10000, "status==SCHEDULED"))
      .thenReturn(mockLegacyCollectionOneItem(jobsWithLegacyBursarParameters));

    bursarMigrationService.recreateLegacyJobs(
      bursarExportLegacyJobService,
      jobService
    );

    Mockito.verify(jobService, times(1)).upsertAndSendToKafka(job, true);
  }

  @Test
  void testRecreateMultipleLegacyJobs() {
    // set up legacy job
    List<JobWithLegacyBursarParameters> jobsWithLegacyBursarParameters = new ArrayList<>();
    JobWithLegacyBursarParameters jobWithLegacyBursarParameters = mockJob();
    jobWithLegacyBursarParameters.setId(UUID.fromString("0000-00-00-00-000000"));
    jobWithLegacyBursarParameters
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setDaysOutstanding(1);

    jobsWithLegacyBursarParameters.add(jobWithLegacyBursarParameters);

    Job job = bursarMigrationService.prepareNewJob();
    job.setId(UUID.fromString("0000-00-00-00-000000"));
    BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
    filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);
    List<BursarExportFilter> filterConditions = new ArrayList<>();
    BursarExportFilterAge ageFilter = new BursarExportFilterAge(); // outstandingDays => ageFilter
    ageFilter.setNumDays(1);
    ageFilter.setCondition(
      BursarExportFilterAge.ConditionEnum.GREATER_THAN_EQUAL
    );
    BursarExportFilterCondition patronGroupListFilter = new BursarExportFilterCondition(); // patronGroups => patronGroupListFilter
    patronGroupListFilter.setOperation(
      BursarExportFilterCondition.OperationEnum.OR
    );
    List<BursarExportFilter> patronGroupFilters = new ArrayList<>();
    patronGroupListFilter.setCriteria(patronGroupFilters);

    filterConditions.add(ageFilter);
    filterConditions.add(patronGroupListFilter);

    filterBase.setCriteria(filterConditions);

    job
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setFilter(filterBase);

    JobWithLegacyBursarParametersCollection legacyCollection = mockLegacyCollectionOneItem(
      jobsWithLegacyBursarParameters
    );
    legacyCollection.setTotalRecords(2);

    Mockito
      .when(bursarExportLegacyJobService.get(0, 10000, "status==SCHEDULED"))
      .thenReturn(legacyCollection);
    Mockito
      .when(bursarExportLegacyJobService.get(1, 10000, "status==SCHEDULED"))
      .thenReturn(legacyCollection);

    bursarMigrationService.recreateLegacyJobs(
      bursarExportLegacyJobService,
      jobService
    );

    Mockito
      .verify(bursarExportLegacyJobService, times(2))
      .get(any(), eq(10000), eq("status==SCHEDULED"));
    Mockito.verify(jobService, times(2)).upsertAndSendToKafka(job, true);
  }

  @Test
  void testMapTypeMappingsToTokens() {
    LegacyBursarFeeFinesTypeMappings typeMappings = new LegacyBursarFeeFinesTypeMappings();
    List<LegacyBursarFeeFinesTypeMapping> typeMappingList = new ArrayList<>();
    LegacyBursarFeeFinesTypeMapping typeMapping = new LegacyBursarFeeFinesTypeMapping();
    typeMapping.setFeefineTypeId(UUID.fromString("0000-00-00-00-000001"));
    typeMapping.setItemType("test_item_type");
    typeMapping.setItemDescription("test_item_description");
    typeMappingList.add(typeMapping);
    typeMappings.put("0000-00-00-00-000001", typeMappingList);

    List<BursarExportTokenConditional> tokens = bursarMigrationService.mapTypeMappingsToTokens(
      typeMappings
    );

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
      UUID.fromString("0000-00-00-00-000001"),
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
      UUID.fromString("0000-00-00-00-000001"),
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

  private JobWithLegacyBursarParameters mockJob() {
    JobWithLegacyBursarParameters job = new JobWithLegacyBursarParameters();
    ExportTypeSpecificParametersWithLegacyBursar nonLegacyExportTypeSpecificParameters = new ExportTypeSpecificParametersWithLegacyBursar();
    LegacyBursarFeeFines nonLegacyBursarFeeFines = new LegacyBursarFeeFines();
    nonLegacyExportTypeSpecificParameters.setBursarFeeFines(nonLegacyBursarFeeFines);
    job.setExportTypeSpecificParameters(
      nonLegacyExportTypeSpecificParameters
    );

    return job;
  }

  private JobWithLegacyBursarParametersCollection mockLegacyCollectionOneItem(
    List<JobWithLegacyBursarParameters> jobsWithLegacyBursarParameters
  ) {
    JobWithLegacyBursarParametersCollection legacyCollection = new JobWithLegacyBursarParametersCollection();

    legacyCollection.setJobRecords(jobsWithLegacyBursarParameters);
    legacyCollection.setTotalRecords(1);

    return legacyCollection;
  }
}
