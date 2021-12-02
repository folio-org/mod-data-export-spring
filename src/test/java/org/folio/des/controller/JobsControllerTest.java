package org.folio.des.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;


@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:job.sql")
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:clearDb.sql")
class JobsControllerTest extends BaseTest {

  private static final String JOB_BURSAR_REQUEST =
      "{\n"
          + "  \"type\": \"BURSAR_FEES_FINES\",\n"
          + "  \"exportTypeSpecificParameters\" : {\n"
          + "    \"bursarFeeFines\": {\n"
          + "        \"daysOutstanding\": 10,\n"
          + "        \"patronGroups\": [\"3684a786-6671-4268-8ed0-9db82ebca60b\"]\n"
          + "    }\n"
          + "}\n"
          + "}";

  private static final String JOB_CIRCULATION_REQUEST =
      "{ \"type\": \"CIRCULATION_LOG\", \"exportTypeSpecificParameters\" : {}}";

  private static final String BULK_EDIT_IDENTIFIERS_REQUEST_NO_IDENTIFIERS_NO_ENTITY =
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, }";

  private static final String BULK_EDIT_IDENTIFIERS_REQUEST_NO_IDENTIFIERS =
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\"}";

  private static final String BULK_EDIT_IDENTIFIERS_REQUEST_NO_ENTITY =
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"identifierType\" : \"ID\"}";

  private static final String BULK_EDIT_UPDATE_REQUEST_NO_IDENTIFIERS_NO_ENTITY =
    "{ \"type\": \"BULK_EDIT_UPDATE\", \"exportTypeSpecificParameters\" : {}}";

  private static final String BULK_EDIT_UPDATE_REQUEST_NO_IDENTIFIERS =
    "{ \"type\": \"BULK_EDIT_UPDATE\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\"}";

  private static final String BULK_EDIT_UPDATE_REQUEST_NO_ENTITY =
    "{ \"type\": \"BULK_EDIT_UPDATE\", \"exportTypeSpecificParameters\" : {}, \"identifierType\" : \"ID\"}";

  @Test
  @DisplayName("Find all jobs")
  void getJobs() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.totalRecords", is(5)),
                jsonPath("$.jobRecords", hasSize(5))));
  }

  @Test
  @DisplayName("Find jobs sorted by name and limited")
  void findSortedJobs() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=3&offset=0&query=(cql.allRecords=1)sortby name/sort.descending")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.totalRecords", is(5)),
                jsonPath("$.jobRecords", hasSize(3))));
  }

  @Test
  @DisplayName("No jobs found cause invalid query")
  void notFoundJobs() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=3&offset=0&query=!!sortby name/sort.descending")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.errors[0].type", is("IllegalArgumentException"))));
  }

  @Test
  @DisplayName("Find jobs by dates")
  void findJobsByQueryDateRange() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(endTime>=2020-12-12T00:00:00.000 and endTime<=2020-12-13T23:59:59.999) sortby name/sort.descending")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.totalRecords", is(0))));
  }

  @Test
  @DisplayName("Find jobs excluding by id")
  void excludeJobById() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(id<>12ae5d0f-1525-44a1-a361-0bc9b88e8179 or name=*)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.totalRecords", is(4)),
              jsonPath("$.jobRecords", hasSize(4))));
  }

  @Test
  @DisplayName("Find jobs by source or desc")
  void findJobsBySourceOrDesc() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(source<>data-export-system-user or description==test-desc)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.totalRecords", is(4)),
              jsonPath("$.jobRecords", hasSize(4))));
  }

  @Test
  @DisplayName("Find jobs by date range")
  void findJobsByStrictDateRange() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(endTime>2020-12-12T00:00:00.000 and endTime<2020-12-13T23:59:59.999)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.totalRecords", is(0))));
  }

  @Test
  @DisplayName("Find jobs by attribute")
  void findJobsAttribute() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(metadata.endTime>2020-12-12T00:00:00.000)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.errors[0].type", is("IllegalArgumentException"))));
  }

  @Test
  @DisplayName("Find jobs by status and type sorted by type")
  void findJobsByQuery() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(status==SUCCESSFUL and isSystemSource==true)sortby type/sort.ascending")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.totalRecords", is(1)),
                jsonPath("$.jobRecords", hasSize(1))));
  }

  @Test
  @DisplayName("Fetch job by id")
  void getJob() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs/12ae5d0f-1525-44a1-a361-0bc9b88e8179")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.id", is("12ae5d0f-1525-44a1-a361-0bc9b88e8179")),
                jsonPath("$.status", is("SUCCESSFUL")),
                jsonPath("$.outputFormat", is("Fees & Fines Bursar Report"))));
  }

  @Test
  @DisplayName("Can not fetch job with wrong id")
  void notFoundJob() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs/12ae5d0f-1525-44a1-a361-0bc9b88eeeee")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpect(
            matchAll(
                status().isNotFound(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.errors[0].type", is("NotFoundException"))));
  }

  @Test
  @DisplayName("Start new bursar export job")
  void postBursarJob() throws Exception {
    mockMvc
        .perform(
            post("/data-export-spring/jobs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders())
                .content(JOB_BURSAR_REQUEST))
        .andExpect(
            matchAll(
                status().isCreated(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.type", is("BURSAR_FEES_FINES")),
                jsonPath("$.status", is("SCHEDULED")),
                jsonPath("$.outputFormat", is("Fees & Fines Bursar Report"))));
  }

  @Test
  @DisplayName("Start new circulation export job")
  void postCirculationJob() throws Exception {
    mockMvc
        .perform(
            post("/data-export-spring/jobs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders())
                .content(JOB_CIRCULATION_REQUEST))
        .andExpect(
            matchAll(
                status().isCreated(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.type", is("CIRCULATION_LOG")),
                jsonPath("$.status", is("SCHEDULED")),
                jsonPath("$.outputFormat", is("Comma-Separated Values (CSV)"))));
  }

  @Test
  @DisplayName("Start new bulk edit identifiers job without identifiers and entity types, should be 404")
  void postBulkEditIdentifiersJobWithNoIdentifiersAndEntityType() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(BULK_EDIT_IDENTIFIERS_REQUEST_NO_IDENTIFIERS_NO_ENTITY))
      .andExpect(
        matchAll(
          status().isBadRequest()));
  }

  @Test
  @DisplayName("Start new bulk edit identifiers job without only identifier type, should be 404")
  void postBulkEditIdentifiersJobWithNoIdentifiersType() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(BULK_EDIT_IDENTIFIERS_REQUEST_NO_IDENTIFIERS))
      .andExpect(
        matchAll(
          status().isBadRequest()));
  }

  @Test
  @DisplayName("Start new bulk edit identifiers job without only entity type, should be 404")
  void postBulkEditIdentifiersJobWithNoEntityType() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(BULK_EDIT_IDENTIFIERS_REQUEST_NO_ENTITY))
      .andExpect(
        matchAll(
          status().isBadRequest()));
  }

  @Test
  @DisplayName("Start new bulk edit update job without identifiers and entity types, should be 404")
  void postBulkEditUpdateJobWithNoIdentifiersAndEntityType() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(BULK_EDIT_UPDATE_REQUEST_NO_IDENTIFIERS_NO_ENTITY))
      .andExpect(
        matchAll(
          status().isBadRequest()));
  }

  @Test
  @DisplayName("Start new bulk edit update job without only identifier type, should be 404")
  void postBulkEditUpdateJobWithNoIdentifiersType() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(BULK_EDIT_UPDATE_REQUEST_NO_IDENTIFIERS))
      .andExpect(
        matchAll(
          status().isBadRequest()));
  }

  @Test
  @DisplayName("Start new bulk edit update job without only entity type, should be 404")
  void postBulkEditUpdateJobWithNoEntityType() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(BULK_EDIT_UPDATE_REQUEST_NO_ENTITY))
      .andExpect(
        matchAll(
          status().isBadRequest()));
  }
}
