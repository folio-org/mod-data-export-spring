package org.folio.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class JobDto extends BaseJobDto {

    private UUID id;

    private JobExecutionDto jobExecution;
}
