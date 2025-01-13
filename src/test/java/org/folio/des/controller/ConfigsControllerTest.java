package org.folio.des.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.folio.des.service.config.ExportConfigConstants.DEFAULT_MODULE_NAME;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static wiremock.org.apache.http.HttpHeaders.CONTENT_TYPE;

import org.folio.des.client.ConfigurationClient;
import org.folio.des.domain.dto.ConfigurationCollection;
import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.domain.dto.ModelConfiguration;
import org.folio.des.scheduling.bursar.BursarExportScheduler;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.Objects;

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

  @Autowired private MockMvc mockMvc;

  @SpyBean private BursarExportScheduler bursarExportScheduler;
  @SpyBean private ConfigurationClient configurationClient;

  @AfterEach
  void clear(){
    reset(bursarExportScheduler, configurationClient);
  }

  @ParameterizedTest
  @CsvSource({
    "/data-export-spring/configs, module==mod-data-export-spring,",
    "/data-export-spring/configs?query=type==BURSAR_FEES_FINES, module==mod-data-export-spring and configName==export_config_parameters,",
    "/data-export-spring/configs?query=type==BATCH_VOUCHER_EXPORT, module==mod-data-export-spring AND value==*BATCH_VOUCHER_EXPORT*,",
    "/data-export-spring/configs?query=type==EDIFACT_ORDERS_EXPORT, module==mod-data-export-spring AND value==*EDIFACT_ORDERS_EXPORT*,",
    "/data-export-spring/configs?query=type==(EDIFACT_ORDERS_EXPORT), module==mod-data-export-spring AND value==*EDIFACT_ORDERS_EXPORT*,",
    "/data-export-spring/configs?query=type==CLAIMS, module==mod-data-export-spring AND value==*CLAIMS*,",
    "/data-export-spring/configs?query=type==(CLAIMS), module==mod-data-export-spring AND value==*CLAIMS*,",
    "/data-export-spring/configs?query=type==(CLAIMS OR EDIFACT_ORDERS_EXPORT), module==mod-data-export-spring AND value==*CLAIMS*, module==mod-data-export-spring AND value==*EDIFACT_ORDERS_EXPORT*",
    "/data-export-spring/configs?query=type==(CLAIMS OR BATCH_VOUCHER_EXPORT), module==mod-data-export-spring AND value==*CLAIMS*, module==mod-data-export-spring AND value==*BATCH_VOUCHER_EXPORT*",
    "/data-export-spring/configs?query=configName==\"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71**\", module==mod-data-export-spring AND configName==\"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71**\",",
    "/data-export-spring/configs?query=configName==\"CLAIMS_1e958895-82a6-4fa1-b6fe-763063381946*\", module==mod-data-export-spring AND configName==\"CLAIMS_1e958895-82a6-4fa1-b6fe-763063381946*\",",
    "/data-export-spring/configs?query=configName==(\"CLAIMS_2e6623c8-e201-48e8-bfae-e6426f04bea3*\" OR \"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71*\"), module==mod-data-export-spring AND configName==(\"CLAIMS_2e6623c8-e201-48e8-bfae-e6426f04bea3*\" OR \"EDIFACT_ORDERS_EXPORT_079e28a8-bf6d-4424-8461-13a3b1c3ec71*\"),",
  })
  @DisplayName("Fetch config by query")
  void getConfigs(String exportConfigQuery, String firstModConfigQuery, String secondModConfigQuery) throws Exception {
    var config = new ConfigurationCollection();
    config.setTotalRecords(0);
    wireMockServer.stubFor(WireMock.get(anyUrl())
      .willReturn(aResponse().withBody(asJsonString(config))
        .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(200)));

    mockMvc
      .perform(
        get(exportConfigQuery)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON_VALUE),
          jsonPath("$.totalRecords", is(0)));

    verify(configurationClient, times(1)).getConfigurations(eq(firstModConfigQuery), any());
    if (Objects.nonNull(secondModConfigQuery)) {
      verify(configurationClient, times(1)).getConfigurations(eq(secondModConfigQuery), any());
    }
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
    wireMockServer.stubFor(WireMock.put(anyUrl())
      .willReturn(aResponse().withBody(UPDATE_CONFIG_REQUEST)
        .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(204)));

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
    wireMockServer.stubFor(WireMock.put(anyUrl())
      .willReturn(aResponse().withBody(UPDATE_CONFIG_REQUEST)
        .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(204)));

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
         jsonPath("$.errors[0].type", is("MethodArgumentNotValidException")));
  }

  @Test
  @DisplayName("Should retrieve config by Id if config exist")
  void shouldRetrieveConfigByIdIfConfigExist() throws Exception {
    String urlById = "/configurations/entries/c8303ff3-7dec-49a1-acc8-7ce4f311fe21";
    ModelConfiguration modelConfiguration = new ModelConfiguration();
    modelConfiguration.setId("c8303ff3-7dec-49a1-acc8-7ce4f311fe21");
    modelConfiguration.setConfigName("First_BURSAR_FEES");
    modelConfiguration.setModule(DEFAULT_MODULE_NAME);
    modelConfiguration.setValue(UPDATE_CONFIG_REQUEST);
    wireMockServer.stubFor(WireMock.get(urlEqualTo(urlById))
      .willReturn(aResponse().withBody(asJsonString(modelConfiguration))
        .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(200)));

    mockMvc
      .perform(
        get("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isOk(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.id", is("c8303ff3-7dec-49a1-acc8-7ce4f311fe21")));

  }

  @Test
  @DisplayName("Should be 404 if config body is null")
  void shouldBe404IfConfigBodyIsNull() throws Exception {
    String urlById = "/configurations/entries/c8303ff3-7dec-49a1-acc8-7ce4f311fe21";
    wireMockServer.stubFor(WireMock.get(urlEqualTo(urlById))
      .willReturn(aResponse().withJsonBody(null)
        .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(200)));

    mockMvc
      .perform(
        get("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isNotFound(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.errors[0].type", is("NotFoundException")));
  }

  @Test
  @DisplayName("Should be 404 if config is not exist")
  void shouldBe404IfConfigIsNotExist() throws Exception {
    String urlById = "/configurations/entries/c8303ff3-7dec-49a1-acc8-7ce4f311fe21";
    wireMockServer.stubFor(WireMock.get(urlEqualTo(urlById))
      .willReturn((aResponse().withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(404))));

    mockMvc
      .perform(
        get("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isNotFound(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.errors[0].type", is("NotFoundException")));
  }

  @Test
  @DisplayName("Should delete config by Id if config exist")
  void shouldDeleteConfigByIdIfConfigExist() throws Exception {
    String urlById = "/configurations/entries/c8303ff3-7dec-49a1-acc8-7ce4f311fe21";
    wireMockServer.stubFor(WireMock.delete(urlEqualTo(urlById))
      .willReturn(noContent().withStatus(204)));

    mockMvc
      .perform(
        delete("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isNoContent());
  }

  @Test
  @DisplayName("Should not be deleted and response 404 if config is not exist")
  void shouldNotBeDeletedIfConfigIsNotExist() throws Exception {
    String urlById = "/configurations/entries/c8303ff3-7dec-49a1-acc8-7ce4f311fe21";
    wireMockServer.stubFor(WireMock.delete(urlEqualTo(urlById))
      .willReturn((aResponse().withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(404))));

    mockMvc
      .perform(
        delete("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(status().isNotFound(),
         content().contentType(MediaType.APPLICATION_JSON_VALUE),
         jsonPath("$.errors[0].type", is("NotFoundException")));
  }
}
