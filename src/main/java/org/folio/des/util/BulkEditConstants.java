package org.folio.des.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BulkEditConstants {
  public static final String MODULE_NAME = "BULKEDIT";
  public static final String CONFIG_NAME = "general";
  public static final String EXPIRATION_PERIOD_PARAMETER = "jobExpirationPeriod";
  public static final String DEFAULT_BULK_EDIT_JOB_EXPIRATION_PERIOD = "14";
  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
  public static final String JOB_EXPIRATION_PERIOD = "jobExpirationPeriod";
}
