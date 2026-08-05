package org.folio.des.domain.dto.event;

import java.util.List;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event-only snapshot of an {@link ExportConfig}. Mirrors the REST DTO field-for-field with the sole exception
 * of the {@code exportTypeSpecificParameters} branch, which is retyped to its redacted event variant so no
 * credential-shaped field can ever be serialized onto the event bus. Enum and week-day types are reused from
 * the REST DTO to guarantee the payload is byte-identical to the REST representation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportConfigEventDto(
    @JsonProperty("id") String id,
    @JsonProperty("type") ExportType type,
    @JsonProperty("configName") String configName,
    @JsonProperty("tenant") String tenant,
    @JsonProperty("exportTypeSpecificParameters") ExportTypeSpecificParametersEventDto exportTypeSpecificParameters,
    @JsonProperty("scheduleFrequency") Integer scheduleFrequency,
    @JsonProperty("schedulePeriod") ExportConfig.SchedulePeriodEnum schedulePeriod,
    @JsonProperty("scheduleTime") String scheduleTime,
    @JsonProperty("weekDays") List<ExportConfig.WeekDaysEnum> weekDays) {
}