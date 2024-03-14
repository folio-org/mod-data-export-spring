package org.folio.des.config.scheduling;

import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class QuartzSchedulerFactoryBeanCustomizer implements SchedulerFactoryBeanCustomizer {
  private final DataSource dataSource;

  /**
   * Use plain datasource instead of folio wrapper for quartz since it does not need tenant specific schemas logic
   */
  @Override
  public void customize(SchedulerFactoryBean schedulerFactoryBean) {
    schedulerFactoryBean.setDataSource(dataSource);
  }
}
