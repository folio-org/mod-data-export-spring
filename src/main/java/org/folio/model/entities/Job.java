package org.folio.model.entities;

import lombok.Data;
import org.folio.dto.JobParameterDto;
import org.folio.model.entities.enums.JobType;

import java.util.Map;
import java.util.UUID;

@Data
public class Job {

    private UUID id;

    private String name;

    private String description;

    private JobType jobType;

    private Map<String, JobParameterDto> jobInputParameters;

    private JobExecution jobExecution;
}
