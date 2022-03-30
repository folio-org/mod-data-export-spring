package org.folio.des.converter.aqcuisition;

import static org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum.NONE;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
import org.folio.des.scheduling.base.ScheduleDateTimeUtil;
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
         var lastJobExecutionDate = getLastJobExecutionDate(scheduleParameters);
         log.info("Last job execution time for config {} is : {}", scheduleParameters.getId(), lastJobExecutionDate);
         var trigger = new AcqBaseExportTaskTrigger(scheduleParameters, lastJobExecutionDate, ediSchedule.getEnableScheduledExport());
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
      if (ediSchedule.isPresent()) {
        ScheduleParameters jobScheduleParameters = ediSchedule.get().getScheduleParameters();
        if (jobScheduleParameters != null && job.getMetadata() != null && job.getMetadata().getCreatedDate() != null) {
          String scheduleTime = jobScheduleParameters.getScheduleTime();
          Date jobCreatedDate = job.getMetadata().getCreatedDate();
          if (StringUtils.isNotEmpty(scheduleTime) && Objects.nonNull(jobCreatedDate)) {
            lastExecutionDate = getConfigScheduledDateConsiderLastJobCreatedDate(jobCreatedDate, scheduleParameters);
          } else {
            lastExecutionDate = getConfigScheduledDate(null, scheduleParameters);
          }
        }
      }
    }
    return lastExecutionDate;
  }

  private Date getConfigScheduledDate(Date lastActualExecutionTime, ScheduleParameters scheduleParameters) {
    ZonedDateTime configStartTime = ScheduleDateTimeUtil.convertScheduleTime(lastActualExecutionTime, scheduleParameters);
    return convertToOldDateFormat(configStartTime, scheduleParameters);
  }

  private Date convertToOldDateFormat(ZonedDateTime configStartTime, ScheduleParameters scheduleParameters) {
    Date lastExecutionDate;
    try {
      lastExecutionDate = ScheduleDateTimeUtil.convertToOldDateFormat(configStartTime, scheduleParameters);
    } catch (ParseException e) {
      log.warn("Configuration start time is not provided for config : " + scheduleParameters.getId());
      return null;
    }
    return lastExecutionDate;
  }

  private Optional<EdiSchedule> getScheduledParameters(ExportTypeSpecificParameters specificParameters) {
    return Optional.ofNullable(specificParameters.getVendorEdiOrdersExportConfig())
      .map(VendorEdiOrdersExportConfig::getEdiSchedule);
  }

  private Date getConfigScheduledDateConsiderLastJobCreatedDate(Date jobCreatedDate, ScheduleParameters scheduleParameters) {
    Calendar jobCal = Calendar.getInstance();
    jobCal.setTime(jobCreatedDate);
    int jobDay = jobCal.get(Calendar.DAY_OF_MONTH);
    int jobHour = jobCal.get(Calendar.HOUR_OF_DAY);

    ZonedDateTime localZoneScheduledDateTime = getScheduledTimeAtLocalTimeZone(scheduleParameters);
    int offsetSeconds = localZoneScheduledDateTime.getOffset().getTotalSeconds();
    int scheduledTimeHour = localZoneScheduledDateTime.plusSeconds(offsetSeconds).getHour();
    Calendar lastExecutionDate = Calendar.getInstance();
    lastExecutionDate.setTime(new Date());
    int currentDayOfMonth = localZoneScheduledDateTime.getDayOfMonth();
    if (jobDay < currentDayOfMonth && jobHour > scheduledTimeHour) {
      lastExecutionDate.set(Calendar.HOUR_OF_DAY, scheduledTimeHour);
    } else {
      lastExecutionDate.set(Calendar.HOUR_OF_DAY, jobHour);
    }
    lastExecutionDate.set(Calendar.MINUTE, localZoneScheduledDateTime.getMinute());
    lastExecutionDate.set(Calendar.SECOND, localZoneScheduledDateTime.getSecond());
    return getConfigScheduledDate(lastExecutionDate.getTime(), scheduleParameters);
  }

  private ZonedDateTime getScheduledTimeAtLocalTimeZone(ScheduleParameters scheduleParameters) {
    LocalTime localScheduledTime = LocalTime.parse(scheduleParameters.getScheduleTime(), DateTimeFormatter.ISO_LOCAL_TIME);
    LocalDate localDate = LocalDate.now();
    LocalDateTime localScheduledDateTime = localDate.atTime(localScheduledTime);
    return ZonedDateTime.of(localScheduledDateTime, ZoneId.systemDefault());
  }

}
