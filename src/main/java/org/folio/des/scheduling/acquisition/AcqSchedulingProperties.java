package org.folio.des.scheduling.acquisition;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class AcqSchedulingProperties {
  @Getter
  private boolean runOnlyIfModuleRegistered;

  @Autowired
  public AcqSchedulingProperties(String runOnlyIfModuleRegistered) {
    this.runOnlyIfModuleRegistered = Boolean.parseBoolean(runOnlyIfModuleRegistered);
  }
}
