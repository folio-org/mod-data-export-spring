package org.folio.des.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.des.domain.dto.ExportConfig;
import org.folio.des.scheduling.RefreshConfigAspect;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ConfigsControllerTest extends BaseTest {

  private static final String NEW_CONFIG_REQUEST =
      "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"exportTypeSpecificParameters\":{\"bursarFeeFines\":{\"daysOutstanding\":10,\"patronGroups\":[\"3684a786-6671-4268-8ed0-9db82ebca60b\"]}},\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";
  private static final String UPDATE_CONFIG_REQUEST =
      "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"exportTypeSpecificParameters\":{\"bursarFeeFines\":{\"daysOutstanding\":10,\"patronGroups\":[\"3684a786-6671-4268-8ed0-9db82ebca60b\"]}},\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";
  private static final String UPDATE_CONFIG_REQUEST_FAILED =
    "{\"id\":\"0a3cba78-16e7-498e-b75b-98713000277b\",\"type\":\"BURSAR_FEES_FINES\",\"scheduleFrequency\":5,\"schedulePeriod\":\"DAY\",\"scheduleTime\":\"00:20:00.000Z\"}";

  @Autowired private MockMvc mockMvc;
  @SpyBean private RefreshConfigAspect configAspect;

  @Test
  @DisplayName("Fetch empty config")
  void getConfigs() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/configs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.totalRecords", is(0))));
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
        .andExpect(
            matchAll(
                status().isCreated(),
                content().contentType("text/plain;charset=UTF-8")));

    verify(configAspect).refreshAfterPost(any(ExportConfig.class));
  }

  @Test
  @DisplayName("Update config")
  void putConfig() throws Exception {
    mockMvc
        .perform(
            put("/data-export-spring/configs/c8303ff3-7dec-49a1-acc8-7ce4f311fe21")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders())
                .content(UPDATE_CONFIG_REQUEST))
        .andExpect(matchAll(status().isOk(), content().contentType("text/plain;charset=UTF-8")));

    verify(configAspect).refreshAfterUpdate(any(ExportConfig.class));
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
        .andExpect(
          matchAll(status().isInternalServerError(),
            content().contentType(MediaType.APPLICATION_JSON_VALUE),
            jsonPath("$.errors[0].type", is("MethodArgumentNotValidException"))));
  }
}
