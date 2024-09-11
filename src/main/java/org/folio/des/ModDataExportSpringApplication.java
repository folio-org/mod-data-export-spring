package org.folio.des;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.JobCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = BatchAutoConfiguration.class)
@EnableFeignClients
@EntityScan(basePackageClasses = JobCommand.class)
public class ModDataExportSpringApplication {
  public static final String SYSTEM_USER_PASSWORD = "SYSTEM_USER_PASSWORD";
  public static final String SYSTEM_USER_ENABLED = "SYSTEM_USER_ENABLED";

  public static void main(String[] args) {
    String systemUserEnabled = Optional.ofNullable(System.getenv(SYSTEM_USER_ENABLED)).orElse("true");
  
    if (
      Boolean.parseBoolean(systemUserEnabled)
      && StringUtils.isEmpty(System.getenv(SYSTEM_USER_PASSWORD))
    ) {
      throw new IllegalArgumentException("Required environment variable is missing: " + SYSTEM_USER_PASSWORD);
    }

    SpringApplication.run(ModDataExportSpringApplication.class, args);
  }

}
