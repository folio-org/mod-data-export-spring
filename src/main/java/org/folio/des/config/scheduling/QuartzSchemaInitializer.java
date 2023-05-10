package org.folio.des.config.scheduling;

import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.stereotype.Component;

import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class QuartzSchemaInitializer implements InitializingBean {
  private final LiquibaseProperties liquibaseProperties;
  private final FolioSpringLiquibase folioSpringLiquibase;

  @Value("${folio.quartz.edifact.enabled}")
  private boolean quartzEnabled;

  @Value("${folio.quartz.schemaName}")
  private String quartzSchemaName;

  @Value("${folio.quartz.changeLog}")
  private String quartzChangeLog;

  /**
   * Performs database update using {@link FolioSpringLiquibase} and then returns previous configuration for this bean.
   *
   * @throws LiquibaseException - if liquibase update failed.
   */
  @Override
  public void afterPropertiesSet() throws LiquibaseException {
    if (!quartzEnabled) {
      log.info("Quartz scheduling is disabled. Schema liquibase initialization for quartz is skipped");
      return;
    }
    folioSpringLiquibase.setChangeLog(quartzChangeLog);
    folioSpringLiquibase.setDefaultSchema(quartzSchemaName);

    folioSpringLiquibase.performLiquibaseUpdate();

    folioSpringLiquibase.setChangeLog(liquibaseProperties.getChangeLog());
    folioSpringLiquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
  }
}
