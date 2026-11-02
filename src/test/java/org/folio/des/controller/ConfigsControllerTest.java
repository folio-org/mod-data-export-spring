package org.folio.des.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Objects;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.config.kafka.KafkaService.Topic;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.event.DomainEvent;
import org.folio.des.domain.dto.event.DomainEventType;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.support.BaseTest;
import org.folio.des.support.TestKafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.SneakyThrows;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=diku_mod_data_export_spring")
class ConfigsControllerTest extends BaseTest {

  private static final String NEW_CONFIG_REQUEST =
      "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"exportTypeSpecificParameters\":{\"bursarFeeFines\":{\"filter\":{\"type\":\"Pass\"},\"groupByPatron\":false,\"header\":[],\"data\":[],\"footer\":[],\"transferInfo\":{\"conditions\":[],\"else\":{\"account\":\"90c1820f-60bf-4b9a-99f5-d677ea78ddca\"}}}},\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";
  // schedulePeriod deliberately differs from NEW_CONFIG_REQUEST (DAY -> HOUR) so the UPDATE event can be
  // asserted to carry an old snapshot that differs from the new one (AC2).
  private static final String UPDATE_CONFIG_REQUEST =
      "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"exportTypeSpecificParameters\":{\"bursarFeeFines\":{\"filter\":{\"type\":\"Pass\"},\"groupByPatron\":false,\"header\":[],\"data\":[],\"footer\":[],\"transferInfo\":{\"conditions\":[],\"else\":{\"account\":\"90c1820f-60bf-4b9a-99f5-d677ea78ddca\"}}}},\"scheduleFrequency\":5,\"schedulePeriod\":\"HOUR\",\"scheduleTime\":\"00:20:00.000Z\"}";
  private static final String FAILED_CONFIG_ID = "c8303ff3-7dec-49a1-acc8-7ce4f311fe21";
  private static final String UPDATE_CONFIG_REQUEST_FAILED =
    "{\"id\":\"c8303ff3-7dec-49a1-acc8-7ce4f311fe21\",\"type\":\"BURSAR_FEES_FINES\",\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";
  private static final String EDIFACT_CONFIG_REQUEST =
    "{\"id\":\"5a3cba28-16e7-498e-b73b-98713000298e\", \"type\": \"EDIFACT_ORDERS_EXPORT\", \"exportTypeSpecificParameters\": { \"vendorEdiOrdersExportConfig\": {\"vendorId\": \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"configName\": \"edi_config\", \"integrationType\": \"Ordering\", \"fileFormat\": \"CSV\", \"transmissionMethod\": \"File download\", \"ediSchedule\": {\"enableScheduledExport\": true, \"scheduleParameters\": {\"scheduleFrequency\": 1, \"schedulePeriod\": \"HOUR\", \"scheduleTime\": \"15:30:00\"}}}}, \"schedulePeriod\": \"HOUR\"}";
  private static final String CLAIMS_REQUEST =
    "{\"id\":\"30ad9c6d-f2e7-425f-a171-b4e0cbce7204\",\"type\":\"CLAIMS\",\"tenant\":\"diku\",\"exportTypeSpecificParameters\":{\"vendorEdiOrdersExportConfig\":{\"exportConfigId\":\"30ad9c6d-f2e7-425f-a171-b4e0cbce7204\",\"vendorId\":\"1e958895-82a6-4fa1-b6fe-763063381946\",\"configName\":\"Test 1-3\",\"ediConfig\":{\"accountNoList\":[\"3\"],\"ediNamingConvention\":\"{organizationCode}-{integrationName}-{exportJobEndDate}\",\"libEdiType\":\"31B/US-SAN\",\"vendorEdiType\":\"31B/US-SAN\",\"sendAccountNumber\":false,\"supportOrder\":false,\"supportInvoice\":false},\"ediFtp\":{\"ftpConnMode\":\"Active\",\"ftpFormat\":\"SFTP\",\"ftpMode\":\"ASCII\"},\"isDefaultConfig\":false,\"integrationType\":\"Claiming\",\"transmissionMethod\":\"File download\",\"fileFormat\":\"CSV\"}},\"schedulePeriod\":\"NONE\"}";
  private static final String EDIFACT_CONFIG_ID = "5a3cba28-16e7-498e-b73b-98713000298e";
  private static final String PO_LINE_ID_1 = "50fb922c-3fa9-494e-a972-f541df9b877e";
  private static final String PO_LINE_ID_2 = "0009662b-8b80-4001-b704-ca10971f222d";
  private static final String EXECUTE_REQUEST =
    "{\"poLineIds\":[\"" + PO_LINE_ID_1 + "\",\"" + PO_LINE_ID_2 + "\"]}";
  private static final String PO_LINE_VENDOR = "046b6c7f-0b8a-43b9-b35d-6489e6daee91";
  private static final String OTHER_VENDOR = "1e958895-82a6-4fa1-b6fe-763063381946";
  private static final String PURCHASE = "c858e4f2-2b6b-4385-842b-60732ee14abb";
  private static final String EXCHANGE = "0a4163a5-d225-4007-ad90-2fb41b73efab";
  private static final String ORDERING_WITH_ACCOUNT_ID = "11111111-1111-4111-8111-111111111111";
  private static final String ORDERING_DEFAULT_ID = "22222222-2222-4222-8222-222222222222";
  private static final String CLAIMS_ID = "33333333-3333-4333-8333-333333333333";
  private static final String CONFIGS_URL = "/data-export-spring/configs";
  private static final String PASSWORD_SENTINEL = "AC3-sentinel-12345";
  private static final String CLAIMS_WITH_PASSWORD_ID = "12345678-1234-1234-1234-1234567890ab";
  private static final String CLAIMS_REQUEST_WITH_PASSWORD =
    "{\"id\":\"12345678-1234-1234-1234-1234567890ab\",\"type\":\"CLAIMS\",\"tenant\":\"diku\",\"exportTypeSpecificParameters\":{\"vendorEdiOrdersExportConfig\":{\"exportConfigId\":\"12345678-1234-1234-1234-1234567890ab\",\"vendorId\":\"1e958895-82a6-4fa1-b6fe-763063381946\",\"configName\":\"Test 1-3\",\"ediConfig\":{\"accountNoList\":[\"3\"],\"ediNamingConvention\":\"{organizationCode}-{integrationName}-{exportJobEndDate}\",\"libEdiType\":\"31B/US-SAN\",\"vendorEdiType\":\"31B/US-SAN\",\"sendAccountNumber\":false,\"supportOrder\":false,\"supportInvoice\":false},\"ediFtp\":{\"ftpConnMode\":\"Active\",\"ftpFormat\":\"SFTP\",\"ftpMode\":\"ASCII\",\"username\":\"ftp-user\",\"password\":\"AC3-sentinel-12345\"},\"isDefaultConfig\":false,\"integrationType\":\"Claiming\",\"transmissionMethod\":\"File download\",\"fileFormat\":\"CSV\"}},\"schedulePeriod\":\"NONE\"}";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private KafkaService kafkaService;
  @Autowired
  private KafkaProperties kafkaProperties;
  @MockitoSpyBean
  private ExportConfigRepository repository;
  @MockitoSpyBean
  private BursarExportScheduler bursarExportScheduler;

  @AfterEach
  void clear(){
    reset(bursarExportScheduler);
    repository.deleteAll();
  }

  @ParameterizedTest
  @CsvSource({
    "/data-export-spring/configs?query=type==BURSAR_FEES_FINES, configName==export_config_parameters,",
    "/data-export-spring/configs?query=type==BATCH_VOUCHER_EXPORT, type==*BATCH_VOUCHER_EXPORT*,",
    "/data-export-spring/configs?query=type==EDIFACT_ORDERS_EXPORT, type==*EDIFACT_ORDERS_EXPORT*,",
    "/data-export-spring/configs?query=type==(EDIFACT_ORDERS_EXPORT), type==*EDIFACT_ORDERS_EXPORT*,",
    "/data-export-spring/configs?query=type==CLAIMS, type==*CLAIMS*,",
    "/data-export-spring/configs?query=type==(CLAIMS), type==*CLAIMS*,",
    "/data-export-spring/configs?query=type==(CLAIMS OR EDIFACT_ORDERS_EXPORT), type==*CLAIMS*, type==*EDIFACT_ORDERS_EXPORT*",
    "/data-export-spring/configs?query=type==(CLAIMS OR BATCH_VOUCHER_EXPORT), type==*CLAIMS*, type==*BATCH_VOUCHER_EXPORT*",
    "/data-export-spring/configs?query=configName==\"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71**\", configName==\"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71**\",",
    "/data-export-spring/configs?query=configName==\"CLAIMS_1e958895-82a6-4fa1-b6fe-763063381946*\", configName==\"CLAIMS_1e958895-82a6-4fa1-b6fe-763063381946*\",",
    "/data-export-spring/configs?query=configName==(\"CLAIMS_2e6623c8-e201-48e8-bfae-e6426f04bea3*\" OR \"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71*\"), configName==(\"CLAIMS_2e6623c8-e201-48e8-bfae-e6426f04bea3*\" OR \"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71*\"),",
  })
  @DisplayName("Fetch config by query")
  void getConfigs(String exportConfigQuery, String firstModConfigQuery, String secondModConfigQuery) throws Exception {
    mockMvc
      .perform(
        get(exportConfigQuery)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON_VALUE),
          jsonPath("$.totalRecords", is(0)));

    verify(repository).findByCql(eq(firstModConfigQuery), any());
    if (Objects.nonNull(secondModConfigQuery)) {
      verify(repository).findByCql(eq(secondModConfigQuery), any());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "/data-export-spring/configs?query=type==(CLAIMS OR EDIFACT_ORDERS_EXPORT)",
    "/data-export-spring/configs?query=type==(CLAIMS Or EDIFACT_ORDERS_EXPORT)",
    "/data-export-spring/configs?query=type==(CLAIMS oR EDIFACT_ORDERS_EXPORT)",
    "/data-export-spring/configs?query=type==(CLAIMS or EDIFACT_ORDERS_EXPORT)",
  })
  @DisplayName("Fetch config by query with case-insensitive operator")
  void getConfigsCaseWithInsensitiveOperators(String exportConfigQuery) throws Exception {
    mockMvc
      .perform(
        get(exportConfigQuery)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.totalRecords", is(0)));

    verify(repository).findByCql(eq("type==*CLAIMS*"), any());
    verify(repository).findByCql(eq("type==*EDIFACT_ORDERS_EXPORT*"), any());
  }

  @ParameterizedTest
  @CsvSource({
    "/data-export-spring/configs?query=type==(CLAIMS OR CLAIMS), type==*CLAIMS*",
    "/data-export-spring/configs?query=type==(EDIFACT_ORDERS_EXPORT OR EDIFACT_ORDERS_EXPORT), type==*EDIFACT_ORDERS_EXPORT*",
  })
  @DisplayName("Fetch config by query with duplicate export type")
  void getConfigsWithDuplicateExportTypes(String exportConfigQuery, String modConfigQuery) throws Exception {
    mockMvc
      .perform(
        get(exportConfigQuery)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.totalRecords", is(0)));

    verify(repository).findByCql(eq(modConfigQuery), any());
  }

  @Test
  @DisplayName("Can not post duplicable config")
  void postConfig() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/configs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(NEW_CONFIG_REQUEST))
      .andExpectAll(status().isCreated(),
          content().contentType("text/plain;charset=UTF-8"));

    verify(bursarExportScheduler).scheduleBursarJob(any(ExportConfig.class));

    var event = pollConfigEventJson("0a3cba78-16e7-498e-b75b-98713000277b", DomainEventType.CREATE);
    assertThat(event.getType()).isEqualTo(DomainEventType.CREATE);
    assertThat(event.getTenant()).isEqualTo(TENANT);
    assertNull(event.getOldValue());
    assertThat(event.getNewValue())
      .usingRecursiveComparison()
      .ignoringFields("configName", "tenant")
      .isEqualTo(OBJECT_MAPPER.readValue(NEW_CONFIG_REQUEST, ExportConfig.class));
  }

  @Test
  @DisplayName("Success posted edifact config")
  void postEdifactConfig() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/configs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(EDIFACT_CONFIG_REQUEST))
      .andExpectAll(status().isCreated(),
          content().contentType("text/plain;charset=UTF-8"));
  }

  @Test
  @DisplayName("Success posted claims config")
  void postClaimsConfig() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/configs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(CLAIMS_REQUEST))
      .andExpectAll(status().isCreated(),
          content().contentType("text/plain;charset=UTF-8"));
  }

  @Test
  @DisplayName("Should redact all credential-shaped fields from the published config event (AC3)")
  void postConfigShouldRedactCredentials() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/configs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(CLAIMS_REQUEST_WITH_PASSWORD))
      .andExpectAll(status().isCreated());

    var eventJson = pollConfigEventRawJson(CLAIMS_WITH_PASSWORD_ID, DomainEventType.CREATE);

    assertThat(eventJson)
      .doesNotContain(PASSWORD_SENTINEL)
      .doesNotContain("\"password\"")
      .doesNotContain("\"secret\"")
      .doesNotContain("\"token\"")
      .doesNotContain("\"apiKey\"")
      .doesNotContain("\"credential\"")
      .doesNotContain("\"privateKey\"");
  }

  @Test
  @DisplayName("Success update config")
  void putConfig() throws Exception {
    saveConfig(NEW_CONFIG_REQUEST);

    mockMvc
        .perform(
            put("/data-export-spring/configs/0a3cba78-16e7-498e-b75b-98713000277b")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders())
                .content(UPDATE_CONFIG_REQUEST))
        .andExpectAll(status().isNoContent());

    verify(bursarExportScheduler).scheduleBursarJob(any(ExportConfig.class));

    var event = pollConfigEventJson("0a3cba78-16e7-498e-b75b-98713000277b", DomainEventType.UPDATE);
    assertThat(event.getType()).isEqualTo(DomainEventType.UPDATE);
    assertThat(event.getTenant()).isEqualTo(TENANT);
    assertThat(event.getOldValue())
      .usingRecursiveComparison()
      .ignoringFields("configName", "tenant")
      .isEqualTo(OBJECT_MAPPER.readValue(NEW_CONFIG_REQUEST, ExportConfig.class));
    assertThat(event.getNewValue())
      .usingRecursiveComparison()
      .ignoringFields("configName", "tenant")
      .isEqualTo(OBJECT_MAPPER.readValue(UPDATE_CONFIG_REQUEST, ExportConfig.class));

    assertThat(event.getOldValue().getSchedulePeriod()).isEqualTo(ExportConfig.SchedulePeriodEnum.DAY);
    assertThat(event.getNewValue().getSchedulePeriod()).isEqualTo(ExportConfig.SchedulePeriodEnum.HOUR);
    assertThat(event.getOldValue().getId()).isEqualTo(event.getNewValue().getId());
  }

  @Test
  @DisplayName("Should throw exception when update config if ID in the body and path is MISMATCH")
  void putShouldThrowExceptionConfig() throws Exception {
    saveConfig(UPDATE_CONFIG_REQUEST);

    mockMvc
      .perform(
        put("/data-export-spring/configs/0a3cba78-16e7-498e-b75b-98713000000b")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(UPDATE_CONFIG_REQUEST))
      .andExpectAll(status().isBadRequest());

  }

  @Test
  @DisplayName("Fail update config and publish no event on validation failure (AC4)")
  void putConfigFail() throws Exception {
    var topic = kafkaService.getTenantTopicName(Topic.CONFIG.getTopicName(), TENANT);
    try (var consumer = TestKafkaConsumer.subscribe(topic, kafkaProperties)) {
      mockMvc
        .perform(
            put("/data-export-spring/configs/" + FAILED_CONFIG_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders())
                .content(UPDATE_CONFIG_REQUEST_FAILED))
        .andExpectAll(status().isBadRequest(),
           content().contentType(MediaType.APPLICATION_JSON_VALUE),
           jsonPath("$.errors[0].message", startsWith("MethodArgumentNotValidException")));

      assertThat(consumer.drainFor(FAILED_CONFIG_ID, Duration.ofSeconds(5))).isEmpty();
    }
  }

  @Test
  @DisplayName("Should retrieve config by Id if config exist")
  void shouldRetrieveConfigByIdIfConfigExist() throws Exception {
    saveConfig(UPDATE_CONFIG_REQUEST);

    mockMvc
      .perform(
        get("/data-export-spring/configs/0a3cba78-16e7-498e-b75b-98713000277b")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.id", is("0a3cba78-16e7-498e-b75b-98713000277b")));

  }

  @Test
  @DisplayName("Should be 404 if config is not exist")
  void shouldBe404IfConfigIsNotExist() throws Exception {
    mockMvc
      .perform(
        get("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isNotFound(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.errors[0].message", startsWith("NotFoundException")));
  }

  @Test
  @DisplayName("Should delete config by Id if config exist")
  void shouldDeleteConfigByIdIfConfigExist() throws Exception {
    saveConfig(NEW_CONFIG_REQUEST);

    mockMvc
      .perform(
        delete("/data-export-spring/configs/0a3cba78-16e7-498e-b75b-98713000277b")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isNoContent());
  }

  @Test
  @DisplayName("Should not be deleted and response 404 if config is not exist")
  void shouldNotBeDeletedIfConfigIsNotExist() throws Exception {
    mockMvc
      .perform(
        delete("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isNotFound(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.errors[0].message", startsWith("NotFoundException")));
  }

  @Test
  @DisplayName("Should run an Ordering edifact config for the given po line ids")
  void shouldRunEdifactConfigForGivenPoLineIds() throws Exception {
    saveConfig(EDIFACT_CONFIG_REQUEST);

    var response = mockMvc
      .perform(
        post("/data-export-spring/configs/" + EDIFACT_CONFIG_ID + "/execute")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(EXECUTE_REQUEST))
      .andExpectAll(status().isCreated(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.jobId").exists())
      .andReturn().getResponse().getContentAsString();

    var jobId = OBJECT_MAPPER.readTree(response).get("jobId").asText();
    mockMvc
      .perform(
        get("/data-export-spring/jobs/" + jobId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
        jsonPath("$.type", is("EDIFACT_ORDERS_EXPORT")),
        // attributed to the triggering user, not to the scheduler
        jsonPath("$.isSystemSource", is(false)),
        jsonPath("$.source", is("diku_admin")),
        jsonPath("$.exportTypeSpecificParameters.vendorEdiOrdersExportConfig.poLineIds",
          contains(PO_LINE_ID_1, PO_LINE_ID_2)));

    // only this endpoint flags the job command as a manual execution for the worker
    assertThat(pollJobCommandRawJson(jobId)).contains("\"manualExecution\"");
  }

  @Test
  @DisplayName("Should not run a Claiming config")
  void shouldNotRunClaimsConfig() throws Exception {
    saveConfig(CLAIMS_REQUEST);

    mockMvc
      .perform(
        post("/data-export-spring/configs/30ad9c6d-f2e7-425f-a171-b4e0cbce7204/execute")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(EXECUTE_REQUEST))
      .andExpectAll(status().isBadRequest(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].code", is("configNotExecutable")));
  }

  @Test
  @DisplayName("Should not run a config that does not exist")
  void shouldNotRunUnknownConfig() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21/execute")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(EXECUTE_REQUEST))
      .andExpectAll(status().isNotFound(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].message", startsWith("NotFoundException")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"poLineIds\":[]}"})
  @DisplayName("Should not run a config without po line ids")
  void shouldNotRunConfigWithoutPoLineIds(String executeRequest) throws Exception {
    saveConfig(EDIFACT_CONFIG_REQUEST);

    mockMvc
      .perform(
        post("/data-export-spring/configs/" + EDIFACT_CONFIG_ID + "/execute")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(executeRequest))
      .andExpectAll(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({
    "111, " + ORDERING_WITH_ACCOUNT_ID,
    "222, " + ORDERING_DEFAULT_ID,
  })
  @DisplayName("PO line params return the matching Ordering config and the vendor's Claims config")
  void getConfigsByCriteria(String vendorAccount, String expectedOrderingId) throws Exception {
    savePoLineConfigs();

    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + PURCHASE + "&vendorAccount=" + vendorAccount)
      .andExpectAll(status().isOk(),
        jsonPath("$.totalRecords", is(2)),
        jsonPath("$.configs[*].id", contains(CLAIMS_ID, expectedOrderingId)));
  }

  @Test
  @DisplayName("PO line without an account and without a matching acquisition method")
  void getConfigsByCriteriaWithoutAccountOrMethod() throws Exception {
    savePoLineConfigs();

    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + PURCHASE)
      .andExpect(jsonPath("$.configs[*].id", contains(CLAIMS_ID, ORDERING_DEFAULT_ID)));
    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + EXCHANGE + "&vendorAccount=111")
      .andExpect(jsonPath("$.configs[*].id", contains(CLAIMS_ID)));
    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR)
      .andExpect(jsonPath("$.configs[*].id", contains(CLAIMS_ID)));
  }

  @Test
  @DisplayName("PO line params: type restricts the result, no match is an empty list")
  void getConfigsByCriteriaByType() throws Exception {
    savePoLineConfigs();

    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + PURCHASE + "&vendorAccount=111&type=EDIFACT_ORDERS_EXPORT")
      .andExpect(jsonPath("$.configs[*].id", contains(ORDERING_WITH_ACCOUNT_ID)));
    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + PURCHASE + "&type=CLAIMS")
      .andExpect(jsonPath("$.configs[*].id", contains(CLAIMS_ID)));
    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + EXCHANGE + "&type=EDIFACT_ORDERS_EXPORT")
      .andExpectAll(status().isOk(), jsonPath("$.totalRecords", is(0)), jsonPath("$.configs", hasSize(0)));
    getPoLineConfigs("vendorId=" + OTHER_VENDOR)
      .andExpectAll(status().isOk(), jsonPath("$.totalRecords", is(0)));
  }

  @ParameterizedTest
  @CsvSource({
    "FTP, " + ORDERING_WITH_ACCOUNT_ID,
    "Email, " + CLAIMS_ID,
    "File download, " + ORDERING_DEFAULT_ID,
  })
  @DisplayName("PO line params: transmissionMethod binds by API value and narrows the result")
  void getConfigsByCriteriaByTransmissionMethod(String method, String expectedId) throws Exception {
    savePoLineConfigs();
    var account = ORDERING_DEFAULT_ID.equals(expectedId) ? "222" : "111";

    mockMvc.perform(get(CONFIGS_URL).headers(defaultHeaders())
        .param("vendorId", PO_LINE_VENDOR)
        .param("acquisitionMethod", PURCHASE)
        .param("vendorAccount", account)
        .param("transmissionMethod", method))
      .andExpectAll(status().isOk(), jsonPath("$.configs[*].id", contains(expectedId)));
  }

  @Test
  @DisplayName("PO line params: Spring also binds the constant name, and an empty value counts as absent")
  void getConfigsByCriteriaTransmissionMethodFallbacks() throws Exception {
    savePoLineConfigs();

    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + PURCHASE + "&transmissionMethod=FILE_DOWNLOAD")
      .andExpectAll(status().isOk(), jsonPath("$.configs[*].id", contains(ORDERING_DEFAULT_ID)));
    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + PURCHASE + "&transmissionMethod=")
      .andExpectAll(status().isOk(), jsonPath("$.configs[*].id", contains(CLAIMS_ID, ORDERING_DEFAULT_ID)));
  }

  @Test
  @DisplayName("PO line params: limit truncates the list, totalRecords stays the number of matches")
  void getConfigsByCriteriaWithLimit() throws Exception {
    savePoLineConfigs();

    getPoLineConfigs("vendorId=" + PO_LINE_VENDOR + "&acquisitionMethod=" + PURCHASE + "&limit=1")
      .andExpectAll(status().isOk(), jsonPath("$.totalRecords", is(2)), jsonPath("$.configs", hasSize(1)));
  }

  @ParameterizedTest
  @CsvSource({
    "acquisitionMethod=" + PURCHASE + ", configVendorIdRequired",
    "vendorAccount=111, configVendorIdRequired",
    "type=CLAIMS, configVendorIdRequired",
    "transmissionMethod=FTP, configVendorIdRequired",
    "vendorId=" + PO_LINE_VENDOR + "&query=type==CLAIMS, configCriteriaWithQuery",
  })
  @DisplayName("PO line params: mode rules return 400")
  void getConfigsByCriteriaModeRules(String params, String expectedCode) throws Exception {
    getPoLineConfigs(params)
      .andExpectAll(status().isBadRequest(), jsonPath("$.errors[0].code", is(expectedCode)));
  }

  @ParameterizedTest
  @CsvSource({
    "type, BURSAR_FEES_FINES",
    "type, unknown",
    "transmissionMethod, unknown",
    "vendorId, not-a-uuid",
    "acquisitionMethod, not-a-uuid",
  })
  @DisplayName("PO line params: unknown enum values and non-UUID ids return 400")
  void getConfigsByCriteriaInvalidValues(String param, String value) throws Exception {
    mockMvc.perform(get(CONFIGS_URL).headers(defaultHeaders()).param(param, value))
      .andExpectAll(status().isBadRequest(),
        jsonPath("$.errors[0].message", startsWith("MethodArgumentTypeMismatchException")));
  }

  @Test
  @DisplayName("Calls without PO line params don't use the matching")
  void getConfigsWithoutPoLineParams() throws Exception {
    savePoLineConfigs();

    mockMvc.perform(get(CONFIGS_URL).headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
        jsonPath("$.configs[*].id", containsInAnyOrder(ORDERING_WITH_ACCOUNT_ID, ORDERING_DEFAULT_ID, CLAIMS_ID,
          "44444444-4444-4444-8444-444444444444")));
    verify(repository, never()).findByTypesAndVendorId(any(), any());
  }

  private ResultActions getPoLineConfigs(String params) throws Exception {
    return mockMvc.perform(get(CONFIGS_URL + "?" + params).headers(defaultHeaders()));
  }

  private void savePoLineConfigs() throws Exception {
    saveConfig(vendorConfig(ORDERING_WITH_ACCOUNT_ID, "EDIFACT_ORDERS_EXPORT", "Ordering", PO_LINE_VENDOR, "FTP", false,
      "[\"111\"]", "\"ediFtp\":{\"serverAddress\":\"sftp://example.org\",\"ftpPort\":22},"));
    saveConfig(vendorConfig(ORDERING_DEFAULT_ID, "EDIFACT_ORDERS_EXPORT", "Ordering", PO_LINE_VENDOR, "File download", true,
      "[]", ""));
    saveConfig(vendorConfig(CLAIMS_ID, "CLAIMS", "Claiming", PO_LINE_VENDOR, "Email", false, "[]", ""));
    saveConfig(vendorConfig("44444444-4444-4444-8444-444444444444", "EDIFACT_ORDERS_EXPORT", "Ordering", OTHER_VENDOR,
      "File download", true, "[]", ""));
  }

  private static String vendorConfig(String id, String type, String integrationType, String vendorId, String method,
                                     boolean isDefault, String accounts, String extra) {
    return """
      {"id":"%s","type":"%s","exportTypeSpecificParameters":{"vendorEdiOrdersExportConfig":{"vendorId":"%s",\
      "configName":"%s","integrationType":"%s","fileFormat":"CSV","transmissionMethod":"%s","isDefaultConfig":%s,%s\
      "ediConfig":{"accountNoList":%s,"defaultAcquisitionMethods":["%s"]}}},"schedulePeriod":"NONE"}"""
      .formatted(id, type, vendorId, id, integrationType, method, isDefault, extra, accounts, PURCHASE);
  }

  private DomainEvent<ExportConfig> pollConfigEventJson(String configId, DomainEventType type) {
    var topic = kafkaService.getTenantTopicName(Topic.CONFIG.getTopicName(), TENANT);
    try (var consumer = TestKafkaConsumer.subscribe(topic, kafkaProperties)) {
      return consumer.poll(configId).stream()
        .map(event -> readEvent(event.value()))
        .filter(event -> event.getType() == type)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected " + type + " event for config " + configId));
    }
  }

  private String pollJobCommandRawJson(String jobId) {
    var topic = kafkaService.getTenantTopicName(Topic.JOB_COMMAND.getTopicName(), TENANT);
    try (var consumer = TestKafkaConsumer.subscribe(topic, kafkaProperties)) {
      return consumer.poll(jobId).stream()
        .map(ConsumerRecord::value)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected job command for job " + jobId));
    }
  }

  private String pollConfigEventRawJson(String configId, DomainEventType type) {
    var topic = kafkaService.getTenantTopicName(Topic.CONFIG.getTopicName(), TENANT);
    try (var consumer = TestKafkaConsumer.subscribe(topic, kafkaProperties)) {
      return consumer.poll(configId).stream()
        .filter(event -> readEvent(event.value()).getType() == type)
        .map(ConsumerRecord::value)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected " + type + " event for config " + configId));
    }
  }

  @SneakyThrows
  private DomainEvent<ExportConfig> readEvent(String json) {
    return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
  }

  private void saveConfig(String config) throws Exception {
    mockMvc.perform(post("/data-export-spring/configs")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .headers(defaultHeaders())
        .content(config))
      .andExpectAll(status().isCreated());
    reset(bursarExportScheduler, repository);
  }
}
