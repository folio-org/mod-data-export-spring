package org.folio.des.converter.aqcuisition;

import static org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum.NONE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.JobCollection;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.acquisition.AcqBaseExportTaskTrigger;
import org.folio.des.scheduling.base.ExportTaskTrigger;
import org.folio.des.service.JobService;
import org.folio.des.validator.acquisition.EdifactOrdersExportParametersValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Service
public class InitEdifactOrdersExportConfigToTaskTriggerConverter implements Converter<ExportConfig, List<ExportTaskTrigger>>  {
  public static final String QUERY_LAST_JOB_CREATE_DATE = "type==EDIFACT_ORDERS_EXPORT and jsonb.exportTypeSpecificParameters.vendorEdiOrdersExportConfig.exportConfigId==%s  sortBy createdDate/sort.descending";
  private EdifactOrdersExportParametersValidator validator;
  private JobService jobService;

  @Override
  public List<ExportTaskTrigger> convert(ExportConfig exportConfig) {
    ExportTypeSpecificParameters specificParameters = exportConfig.getExportTypeSpecificParameters();
    Errors errors = new BeanPropertyBindingResult(specificParameters, "specificParameters");
    validator.validate(specificParameters, errors);

    List<ExportTaskTrigger> exportTaskTriggers = new ArrayList<>(1);
    getScheduledParameters(specificParameters).ifPresent(ediSchedule -> {
        ScheduleParameters scheduleParameters = ediSchedule.getScheduleParameters();
        if (scheduleParameters != null && !NONE.equals(scheduleParameters.getSchedulePeriod())) {
         if (scheduleParameters.getId() == null) {
           scheduleParameters.setId(UUID.fromString(exportConfig.getId()));
         }
         scheduleParameters.setTimeZone(scheduleParameters.getTimeZone());
         var lasJobExecutionDate = getLastJobExecutionDate(scheduleParameters);
         var trigger = new AcqBaseExportTaskTrigger(scheduleParameters, lasJobExecutionDate, ediSchedule.getEnableScheduledExport());
         exportTaskTriggers.add(trigger);
       }
    });
    return exportTaskTriggers;
  }

  private Date getLastJobExecutionDate(ScheduleParameters scheduleParameters) {
    String query = String.format(QUERY_LAST_JOB_CREATE_DATE, scheduleParameters.getId());
    JobCollection jobCol = jobService.get(0, Integer.MAX_VALUE, query);
    Date lastExecutionDate = null;
    Optional<Job> jobOptional = jobCol.getJobRecords().stream().findFirst();
    if (jobOptional.isPresent()) {
      Job job = jobOptional.get();
      var ediSchedule = getScheduledParameters(job.getExportTypeSpecificParameters());
      if (ediSchedule.isPresent() ) {
        ScheduleParameters jobScheduleParameters = ediSchedule.get().getScheduleParameters();
        if (jobScheduleParameters != null && job.getMetadata() != null) {
          lastExecutionDate = job.getMetadata().getCreatedDate();
        }
      }
    }
    return lastExecutionDate;
  }

  private Optional<EdiSchedule> getScheduledParameters(ExportTypeSpecificParameters specificParameters) {
    return Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
      .map(VendorEdiOrdersExportConfig::getEdiSchedule);
  }
}
