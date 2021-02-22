package org.folio.des.domain.entity;

import lombok.Data;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

import java.util.Date;

@Data
public class JobExecution {

  private BatchStatus status;
  private Date startTime;
  private Date createTime;
  private Date endTime;
  private Date lastUpdated;
  private ExitStatus exitStatus;

}
