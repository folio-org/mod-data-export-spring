package org.folio.des.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=diku_mod_data_export_spring")
class ConfigsControllerTest extends BaseTest {

  private static final String NEW_CONFIG_REQUEST =
      "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"exportTypeSpecificParameters\":{\"bursarFeeFines\":{\"filter\":{\"type\":\"Pass\"},\"groupByPatron\":false,\"header\":[],\"data\":[],\"footer\":[],\"transferInfo\":{\"conditions\":[],\"else\":{\"account\":\"90c1820f-60bf-4b9a-99f5-d677ea78ddca\"}}}},\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";
  private static final String UPDATE_CONFIG_REQUEST =
      "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"exportTypeSpecificParameters\":{\"bursarFeeFines\":{\"filter\":{\"type\":\"Pass\"},\"groupByPatron\":false,\"header\":[],\"data\":[],\"footer\":[],\"transferInfo\":{\"conditions\":[],\"else\":{\"account\":\"90c1820f-60bf-4b9a-99f5-d677ea78ddca\"}}}},\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";
  private static final String UPDATE_CONFIG_REQUEST_FAILED =
    "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";
  private static final String EDIFACT_CONFIG_REQUEST =
    "{\"id\":\"5a3cba28-16e7-498e-b73b-98713000298e\", \"type\": \"EDIFACT_ORDERS_EXPORT\", \"exportTypeSpecificParameters\": { \"vendorEdiOrdersExportConfig\": {\"vendorId\": \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"configName\": \"edi_config\", \"ediSchedule\": {\"enableScheduledExport\": true, \"scheduleParameters\": {\"scheduleFrequency\": 1, \"schedulePeriod\": \"HOUR\", \"scheduleTime\": \"15:30:00\"}}}}, \"schedulePeriod\": \"HOUR\"}";
  private static final String CLAIMS_REQUEST =
    "{\"id\":\"30ad9c6d-f2e7-425f-a171-b4e0cbce7204\",\"type\":\"CLAIMS\",\"tenant\":\"diku\",\"exportTypeSpecificParameters\":{\"vendorEdiOrdersExportConfig\":{\"exportConfigId\":\"30ad9c6d-f2e7-425f-a171-b4e0cbce7204\",\"vendorId\":\"1e958895-82a6-4fa1-b6fe-763063381946\",\"configName\":\"Test 1-3\",\"ediConfig\":{\"accountNoList\":[\"3\"],\"ediNamingConvention\":\"{organizationCode}-{integrationName}-{exportJobEndDate}\",\"libEdiType\":\"31B/US-SAN\",\"vendorEdiType\":\"31B/US-SAN\",\"sendAccountNumber\":false,\"supportOrder\":false,\"supportInvoice\":false},\"ediFtp\":{\"ftpConnMode\":\"Active\",\"ftpFormat\":\"SFTP\",\"ftpMode\":\"ASCII\"},\"isDefaultConfig\":false,\"integrationType\":\"Claiming\",\"transmissionMethod\":\"File download\",\"fileFormat\":\"CSV\"}},\"schedulePeriod\":\"NONE\"}";

  @Autowired
  private MockMvc mockMvc;
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
  @DisplayName("Success update config")
  void putConfig() throws Exception {
    saveConfig(UPDATE_CONFIG_REQUEST);

    mockMvc
        .perform(
            put("/data-export-spring/configs/0a3cba78-16e7-498e-b75b-98713000277b")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders())
                .content(UPDATE_CONFIG_REQUEST))
        .andExpectAll(status().isNoContent());

    verify(bursarExportScheduler).scheduleBursarJob(any(ExportConfig.class));
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
  @DisplayName("Fail update config")
  void putConfigFail() throws Exception {
    mockMvc
      .perform(
          put("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .headers(defaultHeaders())
              .content(UPDATE_CONFIG_REQUEST_FAILED))
      .andExpectAll(status().isBadRequest(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.errors[0].message", startsWith("MethodArgumentNotValidException")));
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

  private void saveConfig(String config) throws Exception {
    mockMvc.perform(post("/data-export-spring/configs")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .headers(defaultHeaders())
        .content(config))
      .andExpectAll(status().isCreated());
    reset(bursarExportScheduler, repository);
  }
}
