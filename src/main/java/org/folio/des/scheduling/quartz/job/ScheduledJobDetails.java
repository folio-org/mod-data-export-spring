package org.folio.des.scheduling.quartz.job;

import org.folio.des.domain.dto.Job;
import org.quartz.JobDetail;

public record ScheduledJobDetails(Job job, JobDetail jobDetail) {
}
