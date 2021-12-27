package org.folio.des.scheduling.acquisition;

import java.util.List;

import org.folio.des.config.JacksonConfiguration;
import org.folio.des.config.ServiceConfiguration;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.Job;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { JacksonConfiguration.class, ServiceConfiguration.class})
public class EdifactOrdersExportJobSchedulerTest {
  @Autowired
  EdifactOrdersExportJobScheduler edifactOrdersExportJobScheduler;

  @Test
  void test() {
    ExportConfig exportConfig = new ExportConfig();
    List<Job> jobs = edifactOrdersExportJobScheduler.scheduleExportJob(exportConfig);
    String s = "";
  }
}
