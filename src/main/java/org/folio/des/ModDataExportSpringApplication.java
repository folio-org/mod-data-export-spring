package org.folio.des;

import org.folio.de.entity.JobCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EntityScan(basePackageClasses = JobCommand.class)
public class ModDataExportSpringApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModDataExportSpringApplication.class, args);
  }

}
