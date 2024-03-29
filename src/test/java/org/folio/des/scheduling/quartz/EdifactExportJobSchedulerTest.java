package org.folio.des.scheduling.quartz;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.domain.dto.EdiConfig;
import org.folio.des.domain.dto.EdiFtp;
import org.folio.des.domain.dto.EdiSchedule;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.ScheduleParameters;
import org.folio.des.domain.dto.ScheduleParameters.SchedulePeriodEnum;
import org.folio.des.domain.dto.ScheduleParameters.WeekDaysEnum;
import org.folio.des.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.des.scheduling.ExportJobScheduler;
import org.folio.des.support.BaseTest;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class EdifactExportJobSchedulerTest extends BaseTest {
  private static final String EXPORT_CONFIG_ID = UUID.randomUUID().toString();
  private static final String SCHEDULE_ID = UUID.randomUUID().toString();
  private static final String ACC_TIME = "17:08:39";
  private static final String EXPORT_GROUP = TENANT + "_edifactOrdersExport";

  @Autowired
  private ExportJobScheduler edifactExportJobScheduler;
  @Autowired
  private ScheduledJobsRemover scheduledJobsRemover;

  @Test
  void testSchedule() throws SchedulerException {
    edifactExportJobScheduler.scheduleExportJob(getExportConfig());

    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    MatcherAssert.assertThat(jobKeys, is(Set.of(JobKey.jobKey(SCHEDULE_ID, EXPORT_GROUP))));

    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator().next());
    assertEquals(1, triggers.size());
    assertEquals(EXPORT_GROUP, triggers.get(0).getKey().getGroup());
  }

  @Test
  void testUnschedule() throws SchedulerException {
    ExportConfig config = getExportConfig();
    edifactExportJobScheduler.scheduleExportJob(config);

    ExportConfig disabledConfig = getExportConfig();
    disabledConfig.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters()
      .setSchedulePeriod(ScheduleParameters.SchedulePeriodEnum.NONE);
    edifactExportJobScheduler.scheduleExportJob(disabledConfig);

    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertTrue(CollectionUtils.isEmpty(jobKeys));
  }

  @Test
  void testReschedule() throws SchedulerException {
    ExportConfig config = getExportConfig();
    edifactExportJobScheduler.scheduleExportJob(config);

    ExportConfig reschedulingConfig = getExportConfig();
    edifactExportJobScheduler.scheduleExportJob(reschedulingConfig);

    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertThat(jobKeys, is(Set.of(JobKey.jobKey(SCHEDULE_ID, EXPORT_GROUP))));

    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator().next());
    assertEquals(1, triggers.size());
    assertEquals(EXPORT_GROUP, triggers.get(0).getKey().getGroup());
  }

  @Test
  void testScheduleMultiWeekDays() throws SchedulerException {
    ExportConfig config = getExportConfig();
    config.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters()
      .weekDays(List.of(WeekDaysEnum.THURSDAY, WeekDaysEnum.SUNDAY, WeekDaysEnum.FRIDAY));

    edifactExportJobScheduler.scheduleExportJob(config);

    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertThat(jobKeys, is(Set.of(JobKey.jobKey(SCHEDULE_ID, EXPORT_GROUP))));

    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator().next());
    assertEquals(3, triggers.size());
    assertEquals(3, triggers.stream().filter(t -> EXPORT_GROUP.equals(t.getKey().getGroup())).count());
  }

  @Test
  void testScheduleWithNullScheduleId() throws SchedulerException {
    ExportConfig config = getExportConfig();
    config.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters()
      .setId(null);

    edifactExportJobScheduler.scheduleExportJob(config);

    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertThat(jobKeys, is(Set.of(JobKey.jobKey(EXPORT_CONFIG_ID, EXPORT_GROUP))));

    var triggers = scheduler.getTriggersOfJob(jobKeys.iterator().next());
    assertEquals(1, triggers.size());
    assertEquals(EXPORT_GROUP, triggers.get(0).getKey().getGroup());
  }

  @Test
  void testJobRemovedForRemovedConfiguration() throws SchedulerException {
    ExportConfig config = getExportConfig();
    LocalTime scheduleStartTime = LocalTime.now(ZoneId.systemDefault()).plusSeconds(5);
    config.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters()
      .scheduleTime(scheduleStartTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
      .schedulePeriod(SchedulePeriodEnum.DAY)
      .scheduleFrequency(1)
      .timeZone(ZoneId.systemDefault().getId());

    wireMockServer.stubFor(post(urlEqualTo("/authn/login-with-expiry"))
      .willReturn(aResponse().withStatus(201)
        .withBody("{\"accessTokenExpiration\":\"2050-01-01T23:59:59Z\", \"refreshTokenExpiration\":\"2050-01-01T23:59:59Z\"}")
        .withHeader("Content-Type", "application/json")
        .withHeader("Set-Cookie", "folioAccessToken=AAA-BBB-CCC; Max-Age=600; Expires=Fri, 01 Sep 2030 13:04:35 GMT; Path=/; Secure; HTTPOnly; SameSite=None")));

    edifactExportJobScheduler.scheduleExportJob(config);
    // job should be scheduled
    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertThat(jobKeys, is(Set.of(JobKey.jobKey(SCHEDULE_ID, EXPORT_GROUP))));

    // during job execution it's found that configuration does not exist anymore and
    // job needs to be removed as well (removed integration case)
    wireMockServer.stubFor(get(urlEqualTo(
      "/configurations/entries/" + EXPORT_CONFIG_ID))
      .willReturn(aResponse().withStatus(404)));
    await().pollDelay(1, TimeUnit.SECONDS).timeout(10, TimeUnit.SECONDS).untilAsserted(
      () -> assertEquals(0, scheduler.getJobKeys(GroupMatcher.anyJobGroup()).size()));
  }

  @Test
  void testDeleteJobGroup() throws SchedulerException {
    ExportConfig config = getExportConfig();
    edifactExportJobScheduler.scheduleExportJob(config);

    scheduledJobsRemover.deleteJobs(TENANT);

    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertEquals(0, jobKeys.size());
  }

  @Test
  void testNotDeleteIfJobDontExists() throws SchedulerException {
    ExportConfig config = getExportConfig();
    edifactExportJobScheduler.scheduleExportJob(config);

    scheduledJobsRemover.deleteJobs("");

    var jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    assertEquals(1, jobKeys.size());
  }

  @Test
  void testNotDeleteOtherJobGroup() throws SchedulerException {
    ExportConfig otherConfig = getExportConfig();
    otherConfig.setTenant("tenant-other");
    ExportConfig config = getExportConfig();
    config.setTenant("diku");
    ExportConfig config2 = getExportConfig();
    config2.setTenant("diku");
    config2.getExportTypeSpecificParameters()
      .getVendorEdiOrdersExportConfig()
      .getEdiSchedule()
      .getScheduleParameters()
      .setId(UUID.randomUUID());

    edifactExportJobScheduler.scheduleExportJob(config);
    edifactExportJobScheduler.scheduleExportJob(config2);
    edifactExportJobScheduler.scheduleExportJob(otherConfig);

    scheduledJobsRemover.deleteJobs("diku");

    var jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupStartsWith("tenant-other"+"_"));
    assertEquals(1, jobKeys.size());

    var deletedJobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupStartsWith("diku"+"_"));
    assertEquals(0, deletedJobKeys.size());
  }

  private ExportConfig getExportConfig() {
    UUID vendorId = UUID.randomUUID();
    ExportConfig exportConfig = new ExportConfig();
    exportConfig.setId(EXPORT_CONFIG_ID);
    exportConfig.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    exportConfig.setTenant(TENANT);
    ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
    VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig = new VendorEdiOrdersExportConfig();
    EdiFtp ediFtp = new EdiFtp();

    vendorEdiOrdersExportConfig.setVendorId(vendorId);

    EdiConfig ediConfig = new EdiConfig();
    EdiSchedule accountEdiSchedule = new EdiSchedule();
    accountEdiSchedule.enableScheduledExport(true);
    ScheduleParameters accScheduledParameters = new ScheduleParameters();
    accScheduledParameters.setId(UUID.fromString(SCHEDULE_ID));
    accScheduledParameters.setSchedulePeriod(SchedulePeriodEnum.WEEK);
    accScheduledParameters.setWeekDays(List.of(WeekDaysEnum.THURSDAY));
    accScheduledParameters.setScheduleFrequency(7);
    accScheduledParameters.setScheduleTime(ACC_TIME);
    accScheduledParameters.setTimeZone("Pacific/Midway");
    accountEdiSchedule.scheduleParameters(accScheduledParameters);
    vendorEdiOrdersExportConfig.setEdiSchedule(accountEdiSchedule);
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.addAccountNoListItem("account-22222");
    ediConfig.setLibEdiCode("7659876");
    ediConfig.setLibEdiType(EdiConfig.LibEdiTypeEnum._31B_US_SAN);
    ediConfig.setVendorEdiCode("1694510");
    ediConfig.setVendorEdiType(EdiConfig.VendorEdiTypeEnum._31B_US_SAN);
    ediFtp.setFtpPort(22);

    vendorEdiOrdersExportConfig.setEdiFtp(ediFtp);

    vendorEdiOrdersExportConfig.setEdiConfig(ediConfig);

    parameters.setVendorEdiOrdersExportConfig(vendorEdiOrdersExportConfig);
    exportConfig.exportTypeSpecificParameters(parameters);
    return exportConfig;
  }
}
