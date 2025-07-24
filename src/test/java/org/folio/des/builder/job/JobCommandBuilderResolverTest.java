package org.folio.des.builder.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.de.entity.Job;
import org.folio.des.client.ConfigurationClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.config.scheduling.QuartzSchemaInitializer;
import org.folio.des.domain.dto.AuthorityControlExportConfig;
import org.folio.des.domain.dto.BursarExportFilter;
import org.folio.des.domain.dto.BursarExportFilterAge;
import org.folio.des.domain.dto.BursarExportFilterCondition;
import org.folio.des.domain.dto.BursarExportFilterCondition.OperationEnum;
import org.folio.des.domain.dto.BursarExportFilterPatronGroup;
import org.folio.des.domain.dto.BursarExportJob;
import org.folio.des.domain.dto.EHoldingsExportConfig;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.quartz.Scheduler;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {JacksonConfiguration.class, ServiceConfiguration.class})
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class})
class JobCommandBuilderResolverTest {

  @Autowired
  private JobCommandBuilderResolver resolver;
  @MockitoBean
  private ConfigurationClient client;
  @MockitoBean
  private Scheduler scheduler;
  @MockitoBean
  private QuartzSchemaInitializer quartzSchemaInitializer;

  @ParameterizedTest
  @DisplayName("Should retrieve builder for specific export type if builder is registered in the resolver")
  @CsvSource({
    "BURSAR_FEES_FINES, BursarFeeFinesJobCommandBuilder",
    "CIRCULATION_LOG, CirculationLogJobCommandBuilder",
    "EDIFACT_ORDERS_EXPORT, EdifactOrdersJobCommandBuilder",
    "E_HOLDINGS, EHoldingsJobCommandBuilder",
    "AUTH_HEADINGS_UPDATES, AuthorityControlJobCommandBuilder"
  })
  void shouldRetrieveBuilderForSpecifiedExportTypeIfBuilderIsRegisteredInTheResolver(ExportType exportType,
              String expBuilderClass) {
    Optional<JobCommandBuilder> builder = resolver.resolve(exportType);
    assertEquals(expBuilderClass, builder.get().getClass().getSimpleName());
  }

  @Test
  void shouldNotRetrieveBuilderForFailedLinkedBibUpdates() {
    Optional<JobCommandBuilder> builder = resolver.resolve(ExportType.FAILED_LINKED_BIB_UPDATES);
    assertTrue(builder.isEmpty());
  }

  @Test
  @DisplayName("Should not retrieve builder for specific export type if builder is not registered in the resolver")
  void shouldRetrieveBuilderForSpecifiedExportTypeIfBuilderIsRegisteredInTheResolver() {
    Optional<JobCommandBuilder> builder = resolver.resolve(ExportType.EDIFACT_ORDERS_EXPORT);
    assertTrue(builder.isPresent());
  }

  @ParameterizedTest
  @DisplayName("Should be create jobParameters")
  @CsvSource({
    "BURSAR_FEES_FINES, bursarFeeFines",
    "CIRCULATION_LOG, query",
    "EDIFACT_ORDERS_EXPORT, edifactOrdersExport",
    "E_HOLDINGS, eHoldingsExportConfig",
    "AUTH_HEADINGS_UPDATES, authorityControlExportConfig"
  })
  void shouldBeCreateJobParameters(ExportType exportType, String paramsKey) {
    Optional<JobCommandBuilder> builder = resolver.resolve(exportType);
    Job job = new Job();
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    EHoldingsExportConfig eHoldingsExportConfig = new EHoldingsExportConfig();
    BursarExportJob bursarFeeFines = new BursarExportJob();
    AuthorityControlExportConfig authorityControlExportConfig = new AuthorityControlExportConfig();

    BursarExportFilterAge bursarExportFilterAge = new BursarExportFilterAge();
    bursarExportFilterAge.setNumDays(1);
    BursarExportFilterPatronGroup bursarExportFilterPatronGroup = new BursarExportFilterPatronGroup();
    bursarExportFilterPatronGroup.setPatronGroupId(UUID.fromString("0000-00-00-00-000000"));

    List<BursarExportFilter> bursarExportFilters = new ArrayList<>();
    bursarExportFilters.add(bursarExportFilterPatronGroup);
    bursarExportFilters.add(bursarExportFilterAge);

    BursarExportFilterCondition bursarExportFilterCondition = new BursarExportFilterCondition();
    bursarExportFilterCondition.setCriteria(bursarExportFilters);
    bursarExportFilterCondition.setOperation(OperationEnum.AND);
    bursarFeeFines.setFilter(bursarExportFilterCondition);

    vendorEdiOrdersExportConfig.vendorId(UUID.randomUUID());
    vendorEdiOrdersExportConfig.setConfigName("TestConfig");

    eHoldingsExportConfig.setRecordId("packageId");
    eHoldingsExportConfig.setRecordType(EHoldingsExportConfig.RecordTypeEnum.PACKAGE);
    eHoldingsExportConfig.setTitleSearchFilters("titleFilters");
    eHoldingsExportConfig.setPackageFields(List.of("packageField"));
    eHoldingsExportConfig.setTitleFields(List.of("titleField"));

    authorityControlExportConfig.setFromDate(LocalDate.now());
    authorityControlExportConfig.toDate(LocalDate.now());

    exportTypeSpecificParameters.setQuery("TestQuery");
    exportTypeSpecificParameters.setBursarFeeFines(bursarFeeFines);
    exportTypeSpecificParameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    exportTypeSpecificParameters.seteHoldingsExportConfig(eHoldingsExportConfig);
    exportTypeSpecificParameters.setAuthorityControlExportConfig(authorityControlExportConfig);
    job.setEntityType(EntityType.USER);
    job.setExportTypeSpecificParameters(exportTypeSpecificParameters);

    JobParameters jobParameters = builder.get().buildJobCommand(job);

    assertNotEquals("null", jobParameters.getParameters().get(paramsKey).getValue());
  }
}
