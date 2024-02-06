package org.folio.des.service.bursarlegacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.folio.des.domain.dto.BursarExportTokenUserDataOptional;
import org.folio.des.domain.dto.BursarExportTransferCriteria;
import org.folio.des.domain.dto.BursarExportTransferCriteriaElse;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportConfigWithLegacyBursar;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobWithLegacyBursarParameters;
import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMapping;
import org.folio.des.domain.dto.LegacyBursarFeeFinesTypeMappings;
import org.folio.des.service.JobService;
import org.folio.des.service.config.impl.BurSarFeesFinesExportConfigService;
import org.folio.des.service.util.JobMapperUtil;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class BursarMigrationService {

  private static final String BURSAR_EXPORT_MIGRATION_HEADER_CONSTANT = "LIB02";

  // this ensures the currently scheduled configuration is updated and new jobs are created with the new schema
  public void updateLegacyBursarConfigs(BurSarFeesFinesExportConfigService configService) {
    // there is only one possible configuration for bursar exports
    configService
      .getFirstConfigLegacy()
      .ifPresent((ExportConfigWithLegacyBursar config) -> {
        log.info("updating legacy bursar config: {}", config);

        // will have empty exportTypeSpecificParameters
        ExportConfig updated = configService.getFirstConfig().orElseThrow();

        configService.updateConfig(
          config.getId(),
          updated.exportTypeSpecificParameters(
            convertLegacyJobParameters(config.getExportTypeSpecificParameters().getBursarFeeFines())
          )
        );
      });
  }

  // this ensures that any information about old jobs persisted in the DB makes sense
  public void updateLegacyBursarJobs(
    BursarExportLegacyJobService legacyJobService,
    JobService jobService
  ) {
    List<JobWithLegacyBursarParameters> jobs = legacyJobService.getAllLegacyJobs();
    log.info("found {} legacy jobs to update", jobs.size());

    for (JobWithLegacyBursarParameters legacyJob : jobs) {
      log.info("updating job: {}", legacyJob);

      Job newJob = JobMapperUtil.legacyBursarToNewDto(
        legacyJob,
        convertLegacyJobParameters(legacyJob.getExportTypeSpecificParameters().getBursarFeeFines())
      );

      // upsert recreated job, does not send to kafka despite the name
      jobService.upsertAndSendToKafka(newJob, false);
    }
  }

  public static ExportTypeSpecificParameters convertLegacyJobParameters(LegacyBursarFeeFines legacyParams) {
    ExportTypeSpecificParameters exportTypeSpecificParameters = new ExportTypeSpecificParameters();
    BursarExportJob bursarExportJob = new BursarExportJob();

    bursarExportJob.setFilter(convertLegacyJobFilters(legacyParams));
    bursarExportJob.setHeader(convertLegacyJobHeader());
    bursarExportJob.setData(convertLegacyJobData(legacyParams));
    bursarExportJob.setFooter(null);
    bursarExportJob.setTransferInfo(convertLegacyJobTransfer(legacyParams));

    // new parameters not in legacy jobs
    bursarExportJob.setGroupByPatron(false);
    bursarExportJob.setGroupByPatronFilter(null);

    exportTypeSpecificParameters.setBursarFeeFines(bursarExportJob);

    return exportTypeSpecificParameters;
  }

  private static BursarExportFilterCondition convertLegacyJobFilters(LegacyBursarFeeFines params) {
    // age
    BursarExportFilterAge ageFilter = new BursarExportFilterAge();
    ageFilter.setNumDays(params.getDaysOutstanding());
    ageFilter.setCondition(BursarExportFilterAge.ConditionEnum.GREATER_THAN_EQUAL);
    BursarExportFilterCondition patronGroupListFilter = new BursarExportFilterCondition();
    patronGroupListFilter.setOperation(BursarExportFilterCondition.OperationEnum.OR);

    // patron groups
    List<BursarExportFilter> patronGroupFilters = new ArrayList<>();
    for (String patronGroupId : params.getPatronGroups()) {
      BursarExportFilterPatronGroup patronGroupFilter = new BursarExportFilterPatronGroup();
      patronGroupFilter.setPatronGroupId(UUID.fromString(patronGroupId));
      patronGroupFilters.add(patronGroupFilter);
    }
    patronGroupListFilter.setCriteria(patronGroupFilters);

    // container
    BursarExportFilterCondition filterBase = new BursarExportFilterCondition();
    filterBase.setOperation(BursarExportFilterCondition.OperationEnum.AND);
    filterBase.setCriteria(List.of(ageFilter, patronGroupListFilter));
    return filterBase;
  }

  // the legacy header consisted just of the string "LIB02"
  private static List<BursarExportHeaderFooter> convertLegacyJobHeader() {
    BursarExportTokenConstant constant = new BursarExportTokenConstant();
    constant.setValue(BURSAR_EXPORT_MIGRATION_HEADER_CONSTANT);

    BursarExportTokenConstant newline = new BursarExportTokenConstant();
    constant.setValue("\n");

    return List.of(constant, newline);
  }

  private static List<BursarExportDataToken> convertLegacyJobData(LegacyBursarFeeFines params) {
    List<BursarExportTokenConditional> typeMappingTokens = mapTypeMappingsToTokens(params.getTypeMappings());
    // special if-else tokens for item type and description
    BursarExportTokenConditional itemTypeToken = typeMappingTokens.get(0);
    BursarExportTokenConditional descriptionToken = typeMappingTokens.get(1);

    //user's external id token
    BursarExportTokenUserDataOptional userIDToken = new BursarExportTokenUserDataOptional();
    userIDToken.setValue(BursarExportTokenUserDataOptional.ValueEnum.EXTERNAL_SYSTEM_ID);

    BursarExportTokenLengthControl userIDTokenLengthControl = new BursarExportTokenLengthControl();
    userIDTokenLengthControl.setLength(7);
    userIDTokenLengthControl.setCharacter(" ");
    userIDTokenLengthControl.setDirection(BursarExportTokenLengthControl.DirectionEnum.FRONT);
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
    feeAmountLengthControl.setDirection(BursarExportTokenLengthControl.DirectionEnum.FRONT);
    feeAmountLengthControl.setTruncate(true);
    feeAmountToken.setLengthControl(feeAmountLengthControl);

    // transaction date token
    BursarExportTokenLengthControl dateComponentLengthControl = new BursarExportTokenLengthControl();
    dateComponentLengthControl.setCharacter("0");
    dateComponentLengthControl.setLength(2);
    dateComponentLengthControl.setDirection(BursarExportTokenLengthControl.DirectionEnum.FRONT);
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

    return List.of(
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
    );
  }

  private static BursarExportTransferCriteria convertLegacyJobTransfer(LegacyBursarFeeFines legacyParams) {
    // legacy exports would transfer all to one account, so we don't have any conditions and use
    // the account as the default (else)
    BursarExportTransferCriteriaElse transferElse = new BursarExportTransferCriteriaElse();
    transferElse.setAccount(legacyParams.getTransferAccountId());

    BursarExportTransferCriteria transferCriteria = new BursarExportTransferCriteria();
    transferCriteria.setConditions(List.of());
    transferCriteria.setElse(transferElse);
    return transferCriteria;
  }

  public static List<BursarExportTokenConditional> mapTypeMappingsToTokens(LegacyBursarFeeFinesTypeMappings typeMappings) {
    // item type token and description token
    BursarExportTokenConditional itemTypeToken = new BursarExportTokenConditional();
    BursarExportTokenConditional descriptionToken = new BursarExportTokenConditional();

    List<BursarExportTokenConditionalConditionsInner> itemTypeConditions = new ArrayList<>();
    List<BursarExportTokenConditionalConditionsInner> descriptionConditions = new ArrayList<>();

    itemTypeToken.setConditions(itemTypeConditions);
    descriptionToken.setConditions(descriptionConditions);

    if (typeMappings != null) {
      for (Map.Entry<String, ?> entry : typeMappings.entrySet()) {
        String ownerID = entry.getKey();

        // unfortunately, LegacyBursarFeeFinesTypeMappings is generated by openapi, which gives it only Map<String List>
        // this casts it to the correct type
        @SuppressWarnings("unchecked")
        List<LegacyBursarFeeFinesTypeMapping> typeMappingList = (List<LegacyBursarFeeFinesTypeMapping>) entry.getValue();

        for (LegacyBursarFeeFinesTypeMapping typeMapping : typeMappingList) {
          BursarExportTokenConditionalConditionsInner itemTypeConditionalConditionsInner = new BursarExportTokenConditionalConditionsInner();
          BursarExportTokenConditionalConditionsInner descriptionTokenConditionalConditionsInner = new BursarExportTokenConditionalConditionsInner();

          // item type
          BursarExportFilterCondition andCondition = new BursarExportFilterCondition();
          andCondition.setOperation(BursarExportFilterCondition.OperationEnum.AND);

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
          descriptionTokenConditionalConditionsInner.setValue(descriptionValueToken);

          itemTypeToken.getConditions().add(itemTypeConditionalConditionsInner);
          descriptionToken
            .getConditions()
            .add(descriptionTokenConditionalConditionsInner);
        }
      }
    }

    BursarExportTokenConstant emptyDataToken = new BursarExportTokenConstant();
    emptyDataToken.setValue("            ");
    itemTypeToken.setElse(emptyDataToken);

    BursarExportTokenFeeMetadata feeFineTypeToken = new BursarExportTokenFeeMetadata();
    feeFineTypeToken.setValue(BursarExportTokenFeeMetadata.ValueEnum.NAME);
    descriptionToken.setElse(feeFineTypeToken);

    return new ArrayList<>(Arrays.asList(itemTypeToken, descriptionToken));
  }
}
