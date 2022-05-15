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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}}";

  private static final String BULK_EDIT_IDENTIFIERS_REQUEST_NO_IDENTIFIERS =
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\"}";

  private static final String BULK_EDIT_IDENTIFIERS_REQUEST_NO_ENTITY =
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"identifierType\" : \"ID\"}";

  private static final String BULK_EDIT_QUERY_REQUEST_NO_ENTITY_NO_QUERY =
    "{ \"type\": \"BULK_EDIT_QUERY\", \"exportTypeSpecificParameters\" : {}}";

  private static final String BULK_EDIT_QUERY_REQUEST_WITH_ENTITY_NO_QUERY =
    "{ \"type\": \"BULK_EDIT_QUERY\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\"}";

  private static final String BULK_EDIT_QUERY_REQUEST_WITH_ENTITY_WITH_QUERY =
    "{ \"type\": \"BULK_EDIT_QUERY\", \"exportTypeSpecificParameters\" : {\"query\":\"barcode==123\"}, \"entityType\" : \"USER\"}";

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
                jsonPath("$.totalRecords", is(6)),
                jsonPath("$.jobRecords", hasSize(6))));
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
                jsonPath("$.totalRecords", is(6)),
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
              jsonPath("$.totalRecords", is(5)),
              jsonPath("$.jobRecords", hasSize(5))));
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
              jsonPath("$.totalRecords", is(5)),
              jsonPath("$.jobRecords", hasSize(5))));
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

  @ParameterizedTest
  @ValueSource(strings = {
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\", \"identifierType\" : \"ID\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\", \"identifierType\" : \"USER_NAME\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\", \"identifierType\" : \"EXTERNAL_SYSTEM_ID\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"USER\", \"identifierType\" : \"BARCODE\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"ITEM\", \"identifierType\" : \"ID\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"ITEM\", \"identifierType\" : \"BARCODE\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"ITEM\", \"identifierType\" : \"HRID\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"ITEM\", \"identifierType\" : \"FORMER_IDS\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"ITEM\", \"identifierType\" : \"ACCESSION_NUMBER\"}",
    "{ \"type\": \"BULK_EDIT_IDENTIFIERS\", \"exportTypeSpecificParameters\" : {}, \"entityType\" : \"ITEM\", \"identifierType\" : \"HOLDINGS_RECORD_ID\"}"
  })
  @DisplayName("Start new bulk edit identifiers job with identifiers and entity type, should be 201")
  void postBulkEditIdentifiersJobWithIdentifiersAndEntityType(String contentString) throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(contentString))
      .andExpect(
        matchAll(
          status().isCreated()));
  }

  @ParameterizedTest
  @ValueSource(strings = {BULK_EDIT_QUERY_REQUEST_NO_ENTITY_NO_QUERY, BULK_EDIT_QUERY_REQUEST_WITH_ENTITY_NO_QUERY})
  @DisplayName("Start new bulk edit query job missing required parameters, should be 404")
  void shouldReturnBadRequestWhenRequiredParametersAreMissing(String content) throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(content))
      .andExpect(
        matchAll(
          status().isBadRequest()));
  }

  @Test
  @DisplayName("Start new bulk edit query job with entity type, should be 201")
  void postBulkEditQueryJobWithEntityType() throws Exception {
    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(BULK_EDIT_QUERY_REQUEST_WITH_ENTITY_WITH_QUERY))
      .andExpect(
        matchAll(
          status().isCreated()));
  }

  @ParameterizedTest
  @DisplayName("Test JSONB criteria")
  @CsvSource({
    "jsonb.exportTypeSpecificParameters.vendorEdiOrdersExportConfig.exportConfigId==\"f18d8154-a02f-4414-9c52-c4f9083f1c32\"",
    "jsonb.exportTypeSpecificParameters.vendorEdiOrdersExportConfig.ediFtp.ftpConnMode==\"Active\"",
    "status==\"SCHEDULED\" and type==\"EDIFACT_ORDERS_EXPORT\" and jsonb.exportTypeSpecificParameters.vendorEdiOrdersExportConfig.exportConfigId==\"f18d8154-a02f-4414-9c52-c4f9083f1c32\" and jsonb.exportTypeSpecificParameters.vendorEdiOrdersExportConfig.vendorId==\"11fb627a-cdf1-11e8-a8d5-f2801f1b9fd1\""
  })
  void findJobsByJSONBQuery(String query) throws Exception {
    mockMvc
      .perform(
        get("/data-export-spring/jobs?limit=30&offset=0&query=" + query)
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
  @DisplayName("Test should Throw Exception If JSONBQuery Is Empty")
  void shouldThrowExceptionIfJSONBQueryIsEmpty() throws Exception {
    mockMvc
      .perform(
        get("/data-export-spring/jobs?limit=30&offset=0&query=jsonb==1 and type==\"EDIFACT_ORDERS_EXPORT\"")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpect(
        matchAll(
          status().isBadRequest(),
          content().contentType(MediaType.APPLICATION_JSON_VALUE)));
  }
}
