package org.folio.des.converter.aqcuisition;

import static org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum.NONE;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
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
        if (jobScheduleParameters != null && job.getMetadata() != null && job.getMetadata().getCreatedDate() != null) {
          String scheduleTime = jobScheduleParameters.getScheduleTime();
          Date jobCreatedDate = job.getMetadata().getCreatedDate();
          if (StringUtils.isNotEmpty(scheduleTime)) {
            lastExecutionDate = normalizeJobDate(scheduleTime, jobCreatedDate);
          }
        }
      }
    }
    return lastExecutionDate;
  }

  private Optional<EdiSchedule> getScheduledParameters(ExportTypeSpecificParameters specificParameters) {
    return Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
      .map(VendorEdiOrdersExportConfig::getEdiSchedule);
  }

  private Date normalizeJobDate(String scheduleTime, Date jobCreatedDate) {
    Date lastExecutionDate;
    Calendar jobCal = Calendar.getInstance();
    jobCal.setTime(jobCreatedDate);
    int jobHour = jobCal.get(Calendar.HOUR_OF_DAY);;
    LocalTime localTime = LocalTime.parse(scheduleTime, DateTimeFormatter.ISO_LOCAL_TIME);
    int scheduledTimeMinute = localTime.getMinute();
    int scheduledTimeSec = localTime.getSecond();
    Calendar now = Calendar.getInstance();
    now.setTime(new Date());
    now.set(Calendar.HOUR_OF_DAY, jobHour);
    now.set(Calendar.MINUTE, scheduledTimeMinute);
    now.set(Calendar.SECOND, scheduledTimeSec);
    lastExecutionDate = now.getTime();
    return lastExecutionDate;
  }
}
