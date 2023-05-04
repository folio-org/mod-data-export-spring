package org.folio.des.config.scheduling;

import org.folio.spring.config.DataSourceFolioWrapper;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuartzSchedulerFactoryBeanCustomizer implements SchedulerFactoryBeanCustomizer {
  private final DataSourceFolioWrapper dataSourceFolioWrapper;

  /**
   * Use plain datasource instead of folio wrapper for quartz since it does not need tenant specific schemas logic
   */
  @Override
  public void customize(SchedulerFactoryBean schedulerFactoryBean) {
    schedulerFactoryBean.setDataSource(dataSourceFolioWrapper.getTargetDataSource());
  }
}
