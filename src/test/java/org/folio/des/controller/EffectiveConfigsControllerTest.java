package org.folio.des.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.folio.des.repository.ExportConfigRepository;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=diku_mod_data_export_spring")
class EffectiveConfigsControllerTest extends BaseTest {

  private static final String CONFIGS_URL = "/data-export-spring/configs";
  private static final String EFFECTIVE_CONFIGS_URL = "/data-export-spring/effective-configs";
  private static final String ORDERING = "&type=EDIFACT_ORDERS_EXPORT";
  private static final String CLAIMS = "&type=CLAIMS";
  private static final String VENDOR = "046b6c7f-0b8a-43b9-b35d-6489e6daee91";
  private static final String OTHER_VENDOR = "1e958895-82a6-4fa1-b6fe-763063381946";
  private static final String PURCHASE = "c858e4f2-2b6b-4385-842b-60732ee14abb";
  private static final String EXCHANGE = "0a4163a5-d225-4007-ad90-2fb41b73efab";
  private static final String ORDERING_WITH_ACCOUNT_ID = "11111111-1111-4111-8111-111111111111";
  private static final String ORDERING_DEFAULT_ID = "22222222-2222-4222-8222-222222222222";
  private static final String CLAIMS_ID = "33333333-3333-4333-8333-333333333333";
  private static final String OTHER_VENDOR_CONFIG_ID = "44444444-4444-4444-8444-444444444444";

  @Autowired
  private MockMvc mockMvc;
  @MockitoSpyBean
  private ExportConfigRepository repository;

  @AfterEach
  void clear() {
    repository.deleteAll();
  }

  @ParameterizedTest
  @CsvSource({
    "111, " + ORDERING_WITH_ACCOUNT_ID,
    "222, " + ORDERING_DEFAULT_ID,
  })
  @DisplayName("Returns the matching Ordering config")
  void getEffectiveConfigs(String vendorAccount, String expectedOrderingId) throws Exception {
    saveConfigs();

    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + PURCHASE + "&vendorAccount=" + vendorAccount + ORDERING)
      .andExpectAll(status().isOk(),
        jsonPath("$.totalRecords", is(1)),
        jsonPath("$.configs[*].id", contains(expectedOrderingId)));
  }

  @Test
  @DisplayName("Config criteria without an account and without a matching acquisition method")
  void getEffectiveConfigsWithoutAccountOrMethod() throws Exception {
    saveConfigs();

    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + PURCHASE + ORDERING)
      .andExpect(jsonPath("$.configs[*].id", contains(ORDERING_DEFAULT_ID)));
    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + EXCHANGE + "&vendorAccount=111" + ORDERING)
      .andExpect(jsonPath("$.configs", hasSize(0)));
    getEffectiveConfigs("vendorId=" + VENDOR + ORDERING)
      .andExpect(jsonPath("$.configs", hasSize(0)));
    getEffectiveConfigs("vendorId=" + VENDOR + CLAIMS)
      .andExpect(jsonPath("$.configs[*].id", contains(CLAIMS_ID)));
  }

  @Test
  @DisplayName("type restricts the result, no match is an empty list")
  void getEffectiveConfigsByType() throws Exception {
    saveConfigs();

    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + PURCHASE + "&vendorAccount=111&type=EDIFACT_ORDERS_EXPORT")
      .andExpect(jsonPath("$.configs[*].id", contains(ORDERING_WITH_ACCOUNT_ID)));
    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + PURCHASE + "&type=CLAIMS")
      .andExpect(jsonPath("$.configs[*].id", contains(CLAIMS_ID)));
    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + EXCHANGE + "&type=EDIFACT_ORDERS_EXPORT")
      .andExpectAll(status().isOk(), jsonPath("$.totalRecords", is(0)), jsonPath("$.configs", hasSize(0)));
    getEffectiveConfigs("vendorId=" + UUID.randomUUID() + CLAIMS)
      .andExpectAll(status().isOk(), jsonPath("$.totalRecords", is(0)));
  }

  @ParameterizedTest
  @CsvSource({
    "EDIFACT_ORDERS_EXPORT, FTP, " + ORDERING_WITH_ACCOUNT_ID,
    "CLAIMS, Email, " + CLAIMS_ID,
    "EDIFACT_ORDERS_EXPORT, File download, " + ORDERING_DEFAULT_ID,
  })
  @DisplayName("transmissionMethod binds by API value and narrows the result")
  void getEffectiveConfigsByTransmissionMethod(String type, String method, String expectedId) throws Exception {
    saveConfigs();
    var account = ORDERING_DEFAULT_ID.equals(expectedId) ? "222" : "111";

    mockMvc.perform(get(EFFECTIVE_CONFIGS_URL).headers(defaultHeaders())
        .param("vendorId", VENDOR)
        .param("type", type)
        .param("acquisitionMethod", PURCHASE)
        .param("vendorAccount", account)
        .param("transmissionMethod", method))
      .andExpectAll(status().isOk(), jsonPath("$.configs[*].id", contains(expectedId)));
  }

  @Test
  @DisplayName("Spring also binds the transmissionMethod constant name, and an empty value counts as absent")
  void getEffectiveConfigsTransmissionMethodFallbacks() throws Exception {
    saveConfigs();

    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + PURCHASE + "&transmissionMethod=FILE_DOWNLOAD" + ORDERING)
      .andExpectAll(status().isOk(), jsonPath("$.configs[*].id", contains(ORDERING_DEFAULT_ID)));
    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + PURCHASE + "&transmissionMethod=" + ORDERING)
      .andExpectAll(status().isOk(), jsonPath("$.configs[*].id", contains(ORDERING_DEFAULT_ID)));
  }

  @Test
  @DisplayName("All matches are returned, there is no limit")
  void getEffectiveConfigsWithoutLimit() throws Exception {
    for (int i = 0; i < 11; i++) {
      saveConfig(vendorConfig(UUID.randomUUID().toString(), "CLAIMS", "Claiming", VENDOR, "Email", false, "[]", ""));
    }

    getEffectiveConfigs("vendorId=" + VENDOR + CLAIMS + "&limit=1")
      .andExpectAll(status().isOk(), jsonPath("$.totalRecords", is(11)), jsonPath("$.configs", hasSize(11)));
  }

  @Test
  @DisplayName("A missing vendorId returns 400")
  void getEffectiveConfigsWithoutVendorId() throws Exception {
    getEffectiveConfigs("acquisitionMethod=" + PURCHASE + CLAIMS)
      .andExpectAll(status().isBadRequest(),
        jsonPath("$.errors[0].message", startsWith("MissingServletRequestParameterException")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "&type="})
  @DisplayName("A missing type returns 400, there is no default type")
  void getEffectiveConfigsWithoutType(String type) throws Exception {
    saveConfigs();

    getEffectiveConfigs("vendorId=" + VENDOR + "&acquisitionMethod=" + PURCHASE + type)
      .andExpectAll(status().isBadRequest(),
        jsonPath("$.errors[0].message", startsWith("MissingServletRequestParameterException")));
  }

  @ParameterizedTest
  @CsvSource({
    "type, unknown",
    "type, BURSAR_FEES_FINES",
    "transmissionMethod, unknown",
    "vendorId, not-a-uuid",
    "acquisitionMethod, not-a-uuid",
  })
  @DisplayName("Unknown or unsupported enum values and non-UUID ids return 400")
  void getEffectiveConfigsInvalidValues(String param, String value) throws Exception {
    var request = get(EFFECTIVE_CONFIGS_URL).headers(defaultHeaders()).param(param, value);
    if (!"vendorId".equals(param)) {
      request.param("vendorId", VENDOR);
    }
    if (!"type".equals(param)) {
      request.param("type", "CLAIMS");
    }

    mockMvc.perform(request)
      .andExpectAll(status().isBadRequest(),
        jsonPath("$.errors[0].message", startsWith("MethodArgumentTypeMismatchException")));
  }

  @Test
  @DisplayName("GET /configs ignores the matching params and lists the configs")
  void getConfigsDoesNotMatch() throws Exception {
    saveConfigs();

    mockMvc.perform(get(CONFIGS_URL).headers(defaultHeaders()).param("vendorId", OTHER_VENDOR).param("acquisitionMethod", EXCHANGE))
      .andExpectAll(status().isOk(),
        jsonPath("$.configs[*].id", containsInAnyOrder(ORDERING_WITH_ACCOUNT_ID, ORDERING_DEFAULT_ID, CLAIMS_ID,
          OTHER_VENDOR_CONFIG_ID)));
    verify(repository, never()).findByTypeAndVendorId(any(), any());
  }

  private ResultActions getEffectiveConfigs(String params) throws Exception {
    return mockMvc.perform(get(EFFECTIVE_CONFIGS_URL + "?" + params).headers(defaultHeaders()));
  }

  private void saveConfigs() throws Exception {
    saveConfig(vendorConfig(ORDERING_WITH_ACCOUNT_ID, "EDIFACT_ORDERS_EXPORT", "Ordering", VENDOR, "FTP", false,
      "[\"111\"]", "\"ediFtp\":{\"serverAddress\":\"sftp://example.org\",\"ftpPort\":22},"));
    saveConfig(vendorConfig(ORDERING_DEFAULT_ID, "EDIFACT_ORDERS_EXPORT", "Ordering", VENDOR, "File download", true,
      "[]", ""));
    saveConfig(vendorConfig(CLAIMS_ID, "CLAIMS", "Claiming", VENDOR, "Email", false, "[]", ""));
    saveConfig(vendorConfig(OTHER_VENDOR_CONFIG_ID, "EDIFACT_ORDERS_EXPORT", "Ordering", OTHER_VENDOR, "File download", true,
      "[]", ""));
  }

  private static String vendorConfig(String id, String type, String integrationType, String vendorId, String method,
                                     boolean isDefault, String accounts, String extra) {
    return """
      {"id":"%s","type":"%s","exportTypeSpecificParameters":{"vendorEdiOrdersExportConfig":{"vendorId":"%s",\
      "configName":"%s","integrationType":"%s","fileFormat":"CSV","transmissionMethod":"%s","isDefaultConfig":%s,%s\
      "ediConfig":{"accountNoList":%s,"defaultAcquisitionMethods":["%s"]}}},"schedulePeriod":"NONE"}"""
      .formatted(id, type, vendorId, id, integrationType, method, isDefault, extra, accounts, PURCHASE);
  }

  private void saveConfig(String config) throws Exception {
    mockMvc.perform(post(CONFIGS_URL)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .headers(defaultHeaders())
        .content(config))
      .andExpectAll(status().isCreated());
    reset(repository);
  }

}
