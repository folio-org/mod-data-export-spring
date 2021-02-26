package org.folio.des;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ModDataExportSpringApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModDataExportSpringApplication.class, args);
  }

}
