package org.folio.des.scheduling.acquisition;

public final class ScheduleUtil {
  private ScheduleUtil() {

  }

  public static boolean isJobScheduleAllowed(boolean isRunOnlyIfModuleRegistered, boolean isModuleRegistered) {
    if (isRunOnlyIfModuleRegistered) {
      return isModuleRegistered;
    }
    return true;
  }
}

