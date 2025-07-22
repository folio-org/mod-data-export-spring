package org.folio.des.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.delete_interval.JobDeletionInterval;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.folio.des.service.impl.JobDeletionIntervalServiceImpl.CREATED_BY_SYSTEM;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobDeletionIntervalsControllerTest extends BaseTest {
  @Autowired
  private MockMvc mockMvc;

  @Test
  void getAllJobDeletionIntervalsReturnsCollection() throws Exception {
    mockMvc.perform(get("/data-export-spring/job-deletion-intervals")
        .contentType(APPLICATION_JSON)
        .headers(defaultHeaders())
        .accept(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.totalRecords", is(6)))
      .andExpect(jsonPath("$.jobDeletionIntervals", hasSize(6)))
      .andExpect(jsonPath("$.jobDeletionIntervals[0].exportType", is("CIRCULATION_LOG")))
      .andExpect(jsonPath("$.jobDeletionIntervals[0].retentionDays", is(7)))
      .andExpect(jsonPath("$.jobDeletionIntervals[0].metadata.createdByUserId", is(CREATED_BY_SYSTEM)))
      .andExpect(jsonPath("$.jobDeletionIntervals[1].exportType", is("BURSAR_FEES_FINES")))
      .andExpect(jsonPath("$.jobDeletionIntervals[1].retentionDays", is(7)))
      .andExpect(jsonPath("$.jobDeletionIntervals[1].metadata.createdByUserId", is(CREATED_BY_SYSTEM)))
      .andExpect(jsonPath("$.jobDeletionIntervals[2].exportType", is("EDIFACT_ORDERS_EXPORT")))
      .andExpect(jsonPath("$.jobDeletionIntervals[2].retentionDays", is(730)))
      .andExpect(jsonPath("$.jobDeletionIntervals[2].metadata.createdByUserId", is(CREATED_BY_SYSTEM)))
      .andExpect(jsonPath("$.jobDeletionIntervals[3].exportType", is("CLAIMS")))
      .andExpect(jsonPath("$.jobDeletionIntervals[3].retentionDays", is(730)))
      .andExpect(jsonPath("$.jobDeletionIntervals[3].metadata.createdByUserId", is(CREATED_BY_SYSTEM)))
      .andExpect(jsonPath("$.jobDeletionIntervals[4].exportType", is("E_HOLDINGS")))
      .andExpect(jsonPath("$.jobDeletionIntervals[4].retentionDays", is(7)))
      .andExpect(jsonPath("$.jobDeletionIntervals[4].metadata.createdByUserId", is(CREATED_BY_SYSTEM)))
      .andExpect(jsonPath("$.jobDeletionIntervals[5].exportType", is("AUTH_HEADINGS_UPDATES")))
      .andExpect(jsonPath("$.jobDeletionIntervals[5].retentionDays", is(7)))
      .andExpect(jsonPath("$.jobDeletionIntervals[5].metadata.createdByUserId", is(CREATED_BY_SYSTEM)));
  }

  @Test
  void testJobDeletionIntervalCRUD() throws Exception {
    var interval = new JobDeletionInterval().exportType(ExportType.INVOICE_EXPORT).retentionDays(20);

    // 1. create interval
    assertCreateInterval(interval);

    // 2. check if interval is created with all fields
    interval = assertGetInterval(interval);

    // 3. update interval retention days
    interval.setRetentionDays(500);
    assertUpdateInterval(interval);

    // 4. check if interval is updated with new retention days
    assertGetInterval(interval);

    // 5. delete interval
    assertDeleteInterval(interval.getExportType());

    // 6. check if interval is not exists anymore
    mockMvc.perform(get("/data-export-spring/job-deletion-intervals/INVOICE_EXPORT")
        .contentType(APPLICATION_JSON)
        .headers(defaultHeaders())
        .accept(APPLICATION_JSON))
      .andExpect(status().isNotFound());

  }

  private void assertCreateInterval(JobDeletionInterval interval) throws Exception {
    mockMvc.perform(post("/data-export-spring/job-deletion-intervals")
        .contentType(APPLICATION_JSON)
        .headers(defaultHeaders())
        .content(getObjectMapper().writeValueAsString(interval)))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.exportType", is("INVOICE_EXPORT")))
      .andExpect(jsonPath("$.retentionDays", is(interval.getRetentionDays())))
      .andExpect(jsonPath("$.metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("$.metadata.createdDate", notNullValue()));
  }

  private JobDeletionInterval assertGetInterval(JobDeletionInterval interval) throws Exception {
    ResultActions result = mockMvc.perform(get("/data-export-spring/job-deletion-intervals/INVOICE_EXPORT")
        .contentType(APPLICATION_JSON)
        .headers(defaultHeaders())
        .accept(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.exportType", is("INVOICE_EXPORT")))
      .andExpect(jsonPath("$.retentionDays", is(interval.getRetentionDays())))
      .andExpect(jsonPath("$.metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("$.metadata.createdDate", notNullValue()));

    String content = result.andReturn().getResponse().getContentAsString();
    return getObjectMapper().readValue(content, JobDeletionInterval.class);
  }

  private void assertUpdateInterval(JobDeletionInterval interval) throws Exception {
    mockMvc.perform(put("/data-export-spring/job-deletion-intervals/INVOICE_EXPORT")
        .contentType(APPLICATION_JSON)
        .headers(defaultHeaders())
        .content(getObjectMapper().writeValueAsString(interval)))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.exportType", is("INVOICE_EXPORT")))
      .andExpect(jsonPath("$.retentionDays", is(interval.getRetentionDays())))
      .andExpect(jsonPath("$.metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("$.metadata.updatedDate", notNullValue()));
  }

  private void assertDeleteInterval(ExportType exportType) throws Exception {
    mockMvc.perform(delete("/data-export-spring/job-deletion-intervals/{exportType}", exportType)
        .contentType(APPLICATION_JSON)
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return objectMapper;
  }
}
