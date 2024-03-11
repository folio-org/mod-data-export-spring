package org.folio.des.config.scheduling;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.config.DataSourceFolioWrapper;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class QuartzSchedulerFactoryBeanCustomizer implements SchedulerFactoryBeanCustomizer {

  private final DataSource dataSource;
  private final FolioExecutionContext folioExecutionContext;


  /**
   * Use plain datasource instead of folio wrapper for quartz since it does not need tenant specific schemas logic
   */
  @Override
  public void customize(SchedulerFactoryBean schedulerFactoryBean) {
    DataSourceFolioWrapper wrapper = new DataSourceFolioWrapper(dataSource, folioExecutionContext);
    schedulerFactoryBean.setDataSource(Objects.requireNonNull(wrapper.getTargetDataSource()));
  }
}
