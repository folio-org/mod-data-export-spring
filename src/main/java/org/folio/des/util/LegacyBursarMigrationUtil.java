package org.folio.des.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
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
import org.folio.des.domain.dto.BursarExportTokenUserData;
import org.folio.des.domain.dto.BursarExportTransferCriteria;
import org.folio.des.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.des.domain.dto.BursarExportTransferCriteriaElse;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMapping;
import org.folio.des.domain.dto.LegacyJob;
import org.folio.des.domain.dto.LegacyJobCollection;
import org.folio.des.service.JobService;
import org.folio.des.service.bursarlegacy.BursarExportLegacyJobService;

@Log4j2
public class LegacyBursarMigrationUtil {

  private static final Integer DEFAULT_LIMIT = 10000;

  private static final String BURSAR_EXPORT_MIGRATION_HEADER_CONSTANT = "LIB02";

  public static boolean isLegacyJob(LegacyJob job) {
    LegacyBursarFeeFines bursarFeeFines = job
      .getExportTypeSpecificParameters()
      .getBursarFeeFines();

    return !(
      bursarFeeFines.getPatronGroups().size() == 0 &&
      bursarFeeFines.getTypeMappings() == null &&
      bursarFeeFines.getDaysOutstanding() == null &&
      bursarFeeFines.getTransferAccountId() == null &&
      bursarFeeFines.getFeefineOwnerId() == null &&
      bursarFeeFines.getServicePointId() == null
    );
  }

  public static void recreateLegacyJobs(
    BursarExportLegacyJobService bursarExportLegacyJobService,
    JobService jobService
  ) {
    log.info("searching for legacy jobs");
    LegacyJobCollection response = bursarExportLegacyJobService.get(
      0,
      DEFAULT_LIMIT,
      "status==SCHEDULED"
    );
    List<LegacyJob> jobsToRecreate = new ArrayList<>();

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

    for (LegacyJob legacyJob : jobsToRecreate) {
      log.info("job to recreate: {}", legacyJob);
      if (isLegacyJob(legacyJob)) {
        ExportTypeSpecificParameters newExportTypeSpecificParams = new ExportTypeSpecificParameters();
        newExportTypeSpecificParams.setVendorEdiOrdersExportConfig(
          legacyJob
            .getExportTypeSpecificParameters()
            .getVendorEdiOrdersExportConfig()
        );
        newExportTypeSpecificParams.setQuery(
          legacyJob.getExportTypeSpecificParameters().getQuery()
        );
        newExportTypeSpecificParams.seteHoldingsExportConfig(
          legacyJob.getExportTypeSpecificParameters().geteHoldingsExportConfig()
        );
        newExportTypeSpecificParams.setAuthorityControlExportConfig(
          legacyJob
            .getExportTypeSpecificParameters()
            .getAuthorityControlExportConfig()
        );

        LegacyBursarFeeFines legacyBursarParams = legacyJob
          .getExportTypeSpecificParameters()
          .getBursarFeeFines();

        BursarExportJob bursarExportJob = new BursarExportJob();

        // filter
        BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
        filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);

        List<BursarExportFilter> filterConditions = new ArrayList<>();

        BursarExportFilterAge ageFilter = new BursarExportFilterAge(); // outstandingDays => ageFilter
        ageFilter.setNumDays(legacyBursarParams.getDaysOutstanding());

        BursarExportFilterCondition patronGroupListFilter = new BursarExportFilterCondition(); // patronGroups => patronGroupListFilter
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

        bursarExportJob.setFilter(filterBase);

        // header
        List<BursarExportHeaderFooter> header = new ArrayList<>();
        BursarExportTokenConstant constantHeaderToken = new BursarExportTokenConstant();
        constantHeaderToken.setValue(BURSAR_EXPORT_MIGRATION_HEADER_CONSTANT);
        header.add(constantHeaderToken);
        bursarExportJob.setHeader(header);

        // data
        // user's external id token
        BursarExportTokenUserData userIDToken = new BursarExportTokenUserData();
        userIDToken.setValue(
          BursarExportTokenUserData.ValueEnum.EXTERNAL_SYSTEM_ID
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
        userIdPadding.setValue("    "); // 4 blanks

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

        // transaction date token - MMDDYY
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
        termToken.setValue("    "); // 4 blanks

        // item type token and description token
        BursarExportTokenConditional itemTypeToken = new BursarExportTokenConditional();
        BursarExportTokenConditional descriptionToken = new BursarExportTokenConditional();

        List<BursarExportTokenConditionalConditionsInner> itemTypeConditions = new ArrayList<>();
        List<BursarExportTokenConditionalConditionsInner> descriptionConditions = new ArrayList<>();

        itemTypeToken.setConditions(itemTypeConditions);
        descriptionToken.setConditions(descriptionConditions);

        if (legacyBursarParams.getTypeMappings() != null) {
          for (String ownerID : legacyBursarParams.getTypeMappings().keySet()) {
            for (LegacyBursarFeeFinesTypeMapping typeMapping : (List<LegacyBursarFeeFinesTypeMapping>) legacyBursarParams
              .getTypeMappings()
              .get(ownerID)) {
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
              itemTypeValueToken.setValue(typeMapping.getItemDescription());

              descriptionTokenConditionalConditionsInner.setCondition(
                andCondition
              );
              descriptionTokenConditionalConditionsInner.setValue(
                descriptionValueToken
              );

              itemTypeToken
                .getConditions()
                .add(itemTypeConditionalConditionsInner);
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
        transferInfo.setAccount(legacyBursarParams.getTransferAccountId());

        transferCriteria.setConditions(transferConditions);
        transferCriteria.setElse(transferInfo);

        bursarExportJob.setTransferInfo(transferCriteria);

        // other bursar export parameters
        bursarExportJob.setGroupByPatron(false);
        bursarExportJob.setGroupByPatronFilter(null);

        newExportTypeSpecificParams.setBursarFeeFines(bursarExportJob);

        Job newJob = new Job();
        newJob.setId(legacyJob.getId());
        newJob.setName(legacyJob.getName());
        newJob.setDescription(legacyJob.getDescription());
        newJob.setSource(legacyJob.getSource());
        newJob.setIsSystemSource(legacyJob.getIsSystemSource());
        newJob.setTenant(legacyJob.getTenant());
        newJob.setType(legacyJob.getType());
        newJob.setExportTypeSpecificParameters(newExportTypeSpecificParams);
        newJob.setStatus(legacyJob.getStatus());
        newJob.setFiles(legacyJob.getFiles());
        newJob.setFileNames(legacyJob.getFileNames());
        newJob.setStartTime((legacyJob.getStartTime()));
        newJob.setEndTime(legacyJob.getEndTime());
        newJob.setMetadata(legacyJob.getMetadata());
        newJob.setOutputFormat(legacyJob.getOutputFormat());
        newJob.setErrorDetails(legacyJob.getErrorDetails());
        newJob.setIdentifierType(legacyJob.getIdentifierType());
        newJob.setEntityType(legacyJob.getEntityType());
        newJob.setProgress(legacyJob.getProgress());

        // upsert recreated job
        jobService.upsertAndSendToKafka(newJob, true);
      }
    }

    log.info("recreated {} legacy jobs", jobsToRecreate.size());
  }
}
