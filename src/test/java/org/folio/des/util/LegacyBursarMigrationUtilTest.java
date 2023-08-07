package org.folio.des.util;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
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
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMapping;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMappings;
import org.folio.des.domain.dto.LegacyExportTypeSpecificParameters;
import org.folio.des.domain.dto.LegacyJob;
import org.folio.des.domain.dto.LegacyJobCollection;
import org.folio.des.service.JobService;
import org.folio.des.service.bursarlegacy.BursarExportLegacyJobService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacyBursarMigrationUtilTest {

  @Mock
  private BursarExportLegacyJobService bursarExportLegacyJobService;

  @Mock
  private JobService jobService;

  @Test
  void testIsLegacyJob() {
    LegacyJob nonLegacyJob = mockNonLegacyJob();

    LegacyJob legacyJob = mockNonLegacyJob();
    legacyJob
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setDaysOutstanding(1);

    Assertions.assertAll(
      () -> assertFalse(LegacyBursarMigrationUtil.isLegacyJob(nonLegacyJob)),
      () -> assertTrue(LegacyBursarMigrationUtil.isLegacyJob(legacyJob))
    );
  }

  @Test
  void testRecreateNoLegacyJob() {
    List<LegacyJob> legacyJobs = new ArrayList<>();
    LegacyJob nonLegacyJob = mockNonLegacyJob();
    nonLegacyJob.setId(UUID.fromString("0000-00-00-00-000000"));
    legacyJobs.add(nonLegacyJob);

    Mockito
      .when(bursarExportLegacyJobService.get(0, 10000, "status==SCHEDULED"))
      .thenReturn(mockLegacyJobCollectionOneItem(legacyJobs));

    LegacyBursarMigrationUtil.recreateLegacyJobs(
      bursarExportLegacyJobService,
      jobService
    );

    Mockito
      .verify(jobService, times(0))
      .upsertAndSendToKafka(any(Job.class), eq(true));
  }

  @Test
  void testRecreateOneLegacyJob() {
    List<LegacyJob> legacyJobs = new ArrayList<>();
    LegacyJob legacyJob = mockNonLegacyJob();
    legacyJob.setId(UUID.fromString("0000-00-00-00-000000"));
    legacyJob
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setDaysOutstanding(1);
    legacyJob
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setPatronGroups(Arrays.asList("0000-00-00-00-000001"));
    legacyJobs.add(legacyJob);

    Job job = LegacyBursarMigrationUtil.prepareNewJob();
    job.setId(UUID.fromString("0000-00-00-00-000000"));
    BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
    filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);
    List<BursarExportFilter> filterConditions = new ArrayList<>();
    BursarExportFilterAge ageFilter = new BursarExportFilterAge();
    ageFilter.setNumDays(1);
    ageFilter.setCondition(BursarExportFilterAge.ConditionEnum.GREATER_THAN);
    BursarExportFilterCondition patronGroupListFilter = new BursarExportFilterCondition(); // patronGroups => patronGroupListFilter
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
      .thenReturn(mockLegacyJobCollectionOneItem(legacyJobs));

    LegacyBursarMigrationUtil.recreateLegacyJobs(
      bursarExportLegacyJobService,
      jobService
    );

    Mockito.verify(jobService, times(1)).upsertAndSendToKafka(job, true);
  }

  @Test
  void testRecreateMultipleLegacyJobs() {
    // set up legacy job
    List<LegacyJob> legacyJobs = new ArrayList<>();
    LegacyJob legacyJob = mockNonLegacyJob();
    legacyJob.setId(UUID.fromString("0000-00-00-00-000000"));
    legacyJob
      .getExportTypeSpecificParameters()
      .getBursarFeeFines()
      .setDaysOutstanding(1);

    legacyJobs.add(legacyJob);

    Job job = LegacyBursarMigrationUtil.prepareNewJob();
    job.setId(UUID.fromString("0000-00-00-00-000000"));
    BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
    filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);
    List<BursarExportFilter> filterConditions = new ArrayList<>();
    BursarExportFilterAge ageFilter = new BursarExportFilterAge(); // outstandingDays => ageFilter
    ageFilter.setNumDays(1);
    ageFilter.setCondition(BursarExportFilterAge.ConditionEnum.GREATER_THAN);
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

    LegacyJobCollection legacyJobCollection = mockLegacyJobCollectionOneItem(
      legacyJobs
    );
    legacyJobCollection.setTotalRecords(2);

    Mockito
      .when(bursarExportLegacyJobService.get(0, 10000, "status==SCHEDULED"))
      .thenReturn(legacyJobCollection);
    Mockito
      .when(bursarExportLegacyJobService.get(1, 10000, "status==SCHEDULED"))
      .thenReturn(legacyJobCollection);

    LegacyBursarMigrationUtil.recreateLegacyJobs(
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

    List<BursarExportTokenConditional> tokens = LegacyBursarMigrationUtil.mapTypeMappingsToTokens(
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

  @Test
  void testCannotInstantiate() {
    assertThrows(
      InvocationTargetException.class,
      () -> {
        var constructor =
          LegacyBursarMigrationUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
      }
    );
  }

  private LegacyJob mockNonLegacyJob() {
    LegacyJob nonLegacyJob = new LegacyJob();
    LegacyExportTypeSpecificParameters nonLegExportTypeSpecificParameters = new LegacyExportTypeSpecificParameters();
    LegacyBursarFeeFines nonLegBursarFeeFines = new LegacyBursarFeeFines();
    nonLegExportTypeSpecificParameters.setBursarFeeFines(nonLegBursarFeeFines);
    nonLegacyJob.setExportTypeSpecificParameters(
      nonLegExportTypeSpecificParameters
    );

    return nonLegacyJob;
  }

  private LegacyJobCollection mockLegacyJobCollectionOneItem(
    List<LegacyJob> legacyJobs
  ) {
    LegacyJobCollection legacyJobCollection = new LegacyJobCollection();

    legacyJobCollection.setJobRecords(legacyJobs);
    legacyJobCollection.setTotalRecords(1);

    return legacyJobCollection;
  }
}
