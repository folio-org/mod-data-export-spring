package org.folio.des.service.bursarlegacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.des.domain.dto.BursarExportDataToken;
import org.folio.des.domain.dto.BursarExportFilter;
import org.folio.des.domain.dto.BursarExportFilterAge;
import org.folio.des.domain.dto.BursarExportFilterCondition;
import org.folio.des.domain.dto.BursarExportFilterFeeFineOwner;
import org.folio.des.domain.dto.BursarExportFilterFeeType;
import org.folio.des.domain.dto.BursarExportFilterPatronGroup;
import org.folio.des.domain.dto.BursarExportHeaderFooter;
import org.folio.des.domain.dto.BursarExportJob;
import org.folio.des.domain.dto.BursarExportTokenConditional;
import org.folio.des.domain.dto.BursarExportTokenConditionalConditionsInner;
import org.folio.des.domain.dto.BursarExportTokenConstant;
import org.folio.des.domain.dto.BursarExportTokenDateType;
import org.folio.des.domain.dto.BursarExportTokenFeeAmount;
import org.folio.des.domain.dto.BursarExportTokenFeeDate;
import org.folio.des.domain.dto.BursarExportTokenFeeMetadata;
import org.folio.des.domain.dto.BursarExportTokenLengthControl;
import org.folio.des.domain.dto.BursarExportTokenUserDataOptional;
import org.folio.des.domain.dto.BursarExportTransferCriteria;
import org.folio.des.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.des.domain.dto.BursarExportTransferCriteriaElse;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.JobWithLegacyBursarParametersCollection;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMapping;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMappings;
import org.folio.des.service.JobService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class BursarMigrationService {
  private static final Integer DEFAULT_LIMIT = 10000;

  private static final String BURSAR_EXPORT_MIGRATION_HEADER_CONSTANT = "LIB02";

  public boolean isLegacyJob(JobWithLegacyBursarParameters job) {
    LegacyBursarFeeFines bursarFeeFines = job
      .getExportTypeSpecificParameters()
      .getBursarFeeFines();

    return bursarFeeFines.getDaysOutstanding() != null;
  }

  public void recreateLegacyJobs(
    BursarExportLegacyJobService bursarExportLegacyJobService,
    JobService jobService
  ) {
    log.info("searching for legacy jobs");
    JobWithLegacyBursarParametersCollection response = bursarExportLegacyJobService.get(
      0,
      DEFAULT_LIMIT,
      "status==SCHEDULED"
    );
    List<JobWithLegacyBursarParameters> jobsToRecreate = new ArrayList<>();

    int total = response.getTotalRecords();
    jobsToRecreate.addAll(response.getJobRecords());
    while (jobsToRecreate.size() < total) {
      response =
        bursarExportLegacyJobService.get(
          jobsToRecreate.size(),
          DEFAULT_LIMIT,
          "status==SCHEDULED"
        );
      jobsToRecreate.addAll(response.getJobRecords());
    }

    for (JobWithLegacyBursarParameters jobWithLegacyBursarParameters : jobsToRecreate) {
      log.info("job to recreate: {}", jobWithLegacyBursarParameters);
      if (isLegacyJob(jobWithLegacyBursarParameters)) {
        Job newJob = prepareNewJob();

        newJob
          .getExportTypeSpecificParameters()
          .setVendorEdiOrdersExportConfig(
            jobWithLegacyBursarParameters
              .getExportTypeSpecificParameters()
              .getVendorEdiOrdersExportConfig()
          );
        newJob
          .getExportTypeSpecificParameters()
          .setQuery(jobWithLegacyBursarParameters.getExportTypeSpecificParameters().getQuery());
        newJob
          .getExportTypeSpecificParameters()
          .seteHoldingsExportConfig(
            jobWithLegacyBursarParameters
              .getExportTypeSpecificParameters()
              .geteHoldingsExportConfig()
          );
        newJob
          .getExportTypeSpecificParameters()
          .setAuthorityControlExportConfig(
            jobWithLegacyBursarParameters
              .getExportTypeSpecificParameters()
              .getAuthorityControlExportConfig()
          );

        LegacyBursarFeeFines legacyBursarParams = jobWithLegacyBursarParameters
          .getExportTypeSpecificParameters()
          .getBursarFeeFines();

        // filter
        BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
        filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);
        List<BursarExportFilter> filterConditions = new ArrayList<>();
        BursarExportFilterAge ageFilter = new BursarExportFilterAge();
        ageFilter.setNumDays(legacyBursarParams.getDaysOutstanding());
        ageFilter.setCondition(
          BursarExportFilterAge.ConditionEnum.GREATER_THAN_EQUAL
        );
        BursarExportFilterCondition patronGroupListFilter = new BursarExportFilterCondition();
        patronGroupListFilter.setOperation(
          BursarExportFilterCondition.OperationEnum.OR
        );

        List<BursarExportFilter> patronGroupFilters = new ArrayList<>();
        for (String patronGroupId : legacyBursarParams.getPatronGroups()) {
          BursarExportFilterPatronGroup patronGroupFilter = new BursarExportFilterPatronGroup();
          patronGroupFilter.setPatronGroupId(UUID.fromString(patronGroupId));

          patronGroupFilters.add(patronGroupFilter);
        }

        patronGroupListFilter.setCriteria(patronGroupFilters);

        filterConditions.add(ageFilter);
        filterConditions.add(patronGroupListFilter);

        filterBase.setCriteria(filterConditions);

        newJob
          .getExportTypeSpecificParameters()
          .getBursarFeeFines()
          .setFilter(filterBase);

        // item type token and description token
        List<BursarExportTokenConditional> typeMappingTokens = mapTypeMappingsToTokens(
          legacyBursarParams.getTypeMappings()
        );
        BursarExportTokenConditional itemTypeToken = typeMappingTokens.get(0);
        BursarExportTokenConditional descriptionToken = typeMappingTokens.get(
          1
        );

        newJob
          .getExportTypeSpecificParameters()
          .getBursarFeeFines()
          .getData()
          .set(3, itemTypeToken);
        newJob
          .getExportTypeSpecificParameters()
          .getBursarFeeFines()
          .getData()
          .set(9, descriptionToken);

        newJob
          .getExportTypeSpecificParameters()
          .getBursarFeeFines()
          .getTransferInfo()
          .getElse()
          .setAccount(legacyBursarParams.getTransferAccountId());

        newJob.setId(jobWithLegacyBursarParameters.getId());
        newJob.setName(jobWithLegacyBursarParameters.getName());
        newJob.setDescription(jobWithLegacyBursarParameters.getDescription());
        newJob.setSource(jobWithLegacyBursarParameters.getSource());
        newJob.setIsSystemSource(jobWithLegacyBursarParameters.getIsSystemSource());
        newJob.setTenant(jobWithLegacyBursarParameters.getTenant());
        newJob.setType(jobWithLegacyBursarParameters.getType());
        newJob.setStatus(jobWithLegacyBursarParameters.getStatus());
        newJob.setFiles(jobWithLegacyBursarParameters.getFiles());
        newJob.setFileNames(jobWithLegacyBursarParameters.getFileNames());
        newJob.setStartTime((jobWithLegacyBursarParameters.getStartTime()));
        newJob.setEndTime(jobWithLegacyBursarParameters.getEndTime());
        newJob.setMetadata(jobWithLegacyBursarParameters.getMetadata());
        newJob.setOutputFormat(jobWithLegacyBursarParameters.getOutputFormat());
        newJob.setErrorDetails(jobWithLegacyBursarParameters.getErrorDetails());
        newJob.setIdentifierType(jobWithLegacyBursarParameters.getIdentifierType());
        newJob.setEntityType(jobWithLegacyBursarParameters.getEntityType());
        newJob.setProgress(jobWithLegacyBursarParameters.getProgress());

        // upsert recreated job
        jobService.upsertAndSendToKafka(newJob, true);
      }
    }

    log.info("recreated {} legacy jobs", jobsToRecreate.size());
  }

  public Job prepareNewJob() {
    Job job = new Job();
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    BursarExportJob bursarExportJob = new BursarExportJob();

    // filter
    BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
    filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);

    List<BursarExportFilter> filterConditions = new ArrayList<>();

    BursarExportFilterAge ageFilter = new BursarExportFilterAge();
    ageFilter.setNumDays(null);
    ageFilter.setCondition(
      BursarExportFilterAge.ConditionEnum.GREATER_THAN_EQUAL
    );

    BursarExportFilterCondition patronGroupListFilter = new BursarExportFilterCondition();
    patronGroupListFilter.setOperation(
      BursarExportFilterCondition.OperationEnum.OR
    );
    List<BursarExportFilter> patronGroupFilters = new ArrayList<>();

    patronGroupListFilter.setCriteria(patronGroupFilters);

    filterConditions.add(ageFilter);
    filterConditions.add(patronGroupListFilter);

    filterBase.setCriteria(filterConditions);

    bursarExportJob.setFilter(filterBase);

    // header
    List<BursarExportHeaderFooter> header = new ArrayList<>();
    BursarExportTokenConstant constantHeaderToken = new BursarExportTokenConstant();
    constantHeaderToken.setValue(BURSAR_EXPORT_MIGRATION_HEADER_CONSTANT);
    header.add(constantHeaderToken);
    bursarExportJob.setHeader(header);

    //data
    //user's external id token
    BursarExportTokenUserDataOptional userIDToken = new BursarExportTokenUserDataOptional();
    userIDToken.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.EXTERNAL_SYSTEM_ID
    );

    BursarExportTokenLengthControl userIDTokenLengthControl = new BursarExportTokenLengthControl();
    userIDTokenLengthControl.setLength(7);
    userIDTokenLengthControl.setCharacter(" ");
    userIDTokenLengthControl.setDirection(
      BursarExportTokenLengthControl.DirectionEnum.FRONT
    );
    userIDTokenLengthControl.setTruncate(true);
    userIDToken.setLengthControl(userIDTokenLengthControl);

    BursarExportTokenConstant userIdPadding = new BursarExportTokenConstant();
    userIdPadding.setValue("    ");

    // fee amount token
    BursarExportTokenFeeAmount feeAmountToken = new BursarExportTokenFeeAmount();
    feeAmountToken.setDecimal(true);
    BursarExportTokenLengthControl feeAmountLengthControl = new BursarExportTokenLengthControl();
    feeAmountLengthControl.setLength(9);
    feeAmountLengthControl.setCharacter("0");
    feeAmountLengthControl.setDirection(
      BursarExportTokenLengthControl.DirectionEnum.FRONT
    );
    feeAmountLengthControl.setTruncate(true);
    feeAmountToken.setLengthControl(feeAmountLengthControl);

    // transaction date token
    BursarExportTokenLengthControl dateComponentLengthControl = new BursarExportTokenLengthControl();
    dateComponentLengthControl.setCharacter("0");
    dateComponentLengthControl.setLength(2);
    dateComponentLengthControl.setDirection(
      BursarExportTokenLengthControl.DirectionEnum.FRONT
    );
    dateComponentLengthControl.setTruncate(true);

    String placeholderTimezone = "America/Chicago";

    BursarExportTokenFeeDate monthToken = new BursarExportTokenFeeDate();
    monthToken.setValue(BursarExportTokenDateType.MONTH);
    monthToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.CREATED);
    monthToken.setLengthControl(dateComponentLengthControl);
    monthToken.setTimezone(placeholderTimezone);

    BursarExportTokenFeeDate dayToken = new BursarExportTokenFeeDate();
    dayToken.setValue(BursarExportTokenDateType.DATE);
    dayToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.CREATED);
    dayToken.setLengthControl(dateComponentLengthControl);
    dayToken.setTimezone(placeholderTimezone);

    BursarExportTokenFeeDate yearToken = new BursarExportTokenFeeDate();
    yearToken.setValue(BursarExportTokenDateType.YEAR_SHORT);
    yearToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.CREATED);
    yearToken.setLengthControl(dateComponentLengthControl);
    yearToken.setTimezone(placeholderTimezone);

    // SFS token
    BursarExportTokenConstant sfsToken = new BursarExportTokenConstant();
    sfsToken.setValue("SFS");

    // Term token
    BursarExportTokenConstant termToken = new BursarExportTokenConstant();
    termToken.setValue("    ");

    // item type token and description token
    BursarExportTokenConditional itemTypeToken = new BursarExportTokenConditional();
    BursarExportTokenConditional descriptionToken = new BursarExportTokenConditional();

    List<BursarExportTokenConditionalConditionsInner> itemTypeConditions = new ArrayList<>();
    List<BursarExportTokenConditionalConditionsInner> descriptionConditions = new ArrayList<>();

    itemTypeToken.setConditions(itemTypeConditions);
    descriptionToken.setConditions(descriptionConditions);

    // default for itemType token
    BursarExportTokenConstant emptyDataToken = new BursarExportTokenConstant();
    emptyDataToken.setValue("            ");
    itemTypeToken.setElse(emptyDataToken);

    // default for description token
    BursarExportTokenFeeMetadata feeFineTypeToken = new BursarExportTokenFeeMetadata();
    feeFineTypeToken.setValue(BursarExportTokenFeeMetadata.ValueEnum.NAME);
    descriptionToken.setElse(feeFineTypeToken);

    List<BursarExportDataToken> dataTokens = new ArrayList<>(
      Arrays.asList(
        userIDToken,
        userIdPadding,
        feeAmountToken,
        itemTypeToken,
        monthToken,
        dayToken,
        yearToken,
        sfsToken,
        termToken,
        descriptionToken
      )
    );

    bursarExportJob.setData(dataTokens);

    // footer
    bursarExportJob.setFooter(null);

    // transfer information
    BursarExportTransferCriteria transferCriteria = new BursarExportTransferCriteria();

    List<BursarExportTransferCriteriaConditionsInner> transferConditions = new ArrayList<>();

    BursarExportTransferCriteriaElse transferInfo = new BursarExportTransferCriteriaElse();

    transferCriteria.setConditions(transferConditions);
    transferCriteria.setElse(transferInfo);

    bursarExportJob.setTransferInfo(transferCriteria);

    // other bursar export parameters
    bursarExportJob.setGroupByPatron(false);
    bursarExportJob.setGroupByPatronFilter(null);

    exportTypeSpecificParameters.setBursarFeeFines(bursarExportJob);
    job.setExportTypeSpecificParameters(exportTypeSpecificParameters);

    return job;
  }

  public List<BursarExportTokenConditional> mapTypeMappingsToTokens(
    LegacyBursarFeeFinesTypeMappings typeMappings
  ) {
    // item type token and description token
    BursarExportTokenConditional itemTypeToken = new BursarExportTokenConditional();
    BursarExportTokenConditional descriptionToken = new BursarExportTokenConditional();

    List<BursarExportTokenConditionalConditionsInner> itemTypeConditions = new ArrayList<>();
    List<BursarExportTokenConditionalConditionsInner> descriptionConditions = new ArrayList<>();

    itemTypeToken.setConditions(itemTypeConditions);
    descriptionToken.setConditions(descriptionConditions);

    if (typeMappings != null) {
      for (Map.Entry<String, List> entry : typeMappings.entrySet()) {
        String ownerID = entry.getKey();
        List<LegacyBursarFeeFinesTypeMapping> typeMappingList = entry.getValue();
        for (LegacyBursarFeeFinesTypeMapping typeMapping : typeMappingList) {
          BursarExportTokenConditionalConditionsInner itemTypeConditionalConditionsInner = new BursarExportTokenConditionalConditionsInner();
          BursarExportTokenConditionalConditionsInner descriptionTokenConditionalConditionsInner = new BursarExportTokenConditionalConditionsInner();

          // item type
          BursarExportFilterCondition andCondition = new BursarExportFilterCondition();
          andCondition.setOperation(
            BursarExportFilterCondition.OperationEnum.AND
          );

          BursarExportFilterFeeType feeTypeCondition = new BursarExportFilterFeeType();
          feeTypeCondition.setFeeFineTypeId(typeMapping.getFeefineTypeId());

          BursarExportFilterFeeFineOwner filterFeeFineOwner = new BursarExportFilterFeeFineOwner();
          filterFeeFineOwner.setFeeFineOwner(UUID.fromString(ownerID));

          List<BursarExportFilter> andConditions = new ArrayList<>();
          andConditions.add(feeTypeCondition);
          andConditions.add(filterFeeFineOwner);

          andCondition.setCriteria(andConditions);

          BursarExportTokenConstant itemTypeValueToken = new BursarExportTokenConstant();
          itemTypeValueToken.setValue(typeMapping.getItemType());

          itemTypeConditionalConditionsInner.setCondition(andCondition);
          itemTypeConditionalConditionsInner.setValue(itemTypeValueToken);

          // item description
          BursarExportTokenConstant descriptionValueToken = new BursarExportTokenConstant();
          descriptionValueToken.setValue(typeMapping.getItemDescription());

          descriptionTokenConditionalConditionsInner.setCondition(andCondition);
          descriptionTokenConditionalConditionsInner.setValue(
            descriptionValueToken
          );

          itemTypeToken.getConditions().add(itemTypeConditionalConditionsInner);
          descriptionToken
            .getConditions()
            .add(descriptionTokenConditionalConditionsInner);
        }
      }
    }

    // default for itemType token
    BursarExportTokenConstant emptyDataToken = new BursarExportTokenConstant();
    emptyDataToken.setValue("            ");
    itemTypeToken.setElse(emptyDataToken);

    // default for description token
    BursarExportTokenFeeMetadata feeFineTypeToken = new BursarExportTokenFeeMetadata();
    feeFineTypeToken.setValue(BursarExportTokenFeeMetadata.ValueEnum.NAME);
    descriptionToken.setElse(feeFineTypeToken);

    return new ArrayList<>(Arrays.asList(itemTypeToken, descriptionToken));
  }
}
