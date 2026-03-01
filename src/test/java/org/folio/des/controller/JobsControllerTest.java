package org.folio.des.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.des.client.ExportWorkerClient;
import org.folio.des.config.JacksonConfiguration;
import org.folio.des.domain.dto.AuthorityControlExportConfig;
import org.folio.des.domain.dto.EHoldingsExportConfig;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.ExportTypeSpecificParameters;
import org.folio.des.domain.dto.Job;
import org.folio.des.domain.dto.PresignedUrl;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;


@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:job.sql")
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:clearDb.sql")
@Import(JacksonConfiguration.class)
class JobsControllerTest extends BaseTest {

  @MockitoBean
  ExportWorkerClient exportWorkerClient;
  private static final String JOB_BURSAR_REQUEST =
    """
      {
        "type": "BURSAR_FEES_FINES",
        "exportTypeSpecificParameters" : {
          "bursarFeeFines": {
            "filter": { "type": "Pass" },
            "groupByPatron": false,
            "header": [],
            "data": [],
            "footer": [],
            "transferInfo": {
              "conditions": [],
              "else": { "account": "90c1820f-60bf-4b9a-99f5-d677ea78ddca" }
            }
          }
        }
      }""";

  private static final String JOB_CIRCULATION_REQUEST =
      "{ \"type\": \"CIRCULATION_LOG\", \"exportTypeSpecificParameters\" : {}}";

  private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  @DisplayName("Find all jobs")
  void getJobs() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.totalRecords", is(8)),
                jsonPath("$.jobRecords", hasSize(8)));
  }

  @Test
  @DisplayName("Find jobs sorted by export method name and limited")
  void findSortedJobsByExportMethodName() throws Exception {
    mockMvc
      .perform(
        get("/data-export-spring/jobs?limit=3&offset=0&query=(cql.allRecords=1)sortby jsonb.exportTypeSpecificParameters.vendorEdiOrdersExportConfig.configName/sort.descending")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON_VALUE),
          jsonPath("$.totalRecords", is(8)),
          jsonPath("$.jobRecords", hasSize(3)));
  }

  @Test
  @DisplayName("No jobs found cause invalid query")
  void notFoundJobs() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=3&offset=0&query=!!sortby name/sort.descending")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.errors[0].message", startsWith("IllegalArgumentException")));
  }

  @Test
  @DisplayName("Find jobs by dates")
  void findJobsByQueryDateRange() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(endTime>=2020-12-12T00:00:00.000 and endTime<=2020-12-13T23:59:59.999) sortby name/sort.descending")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.totalRecords", is(0)));
  }

  @Test
  @DisplayName("Find jobs excluding by id")
  void excludeJobById() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(id<>12ae5d0f-1525-44a1-a361-0bc9b88e8179 or name=*)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.totalRecords", is(7)),
              jsonPath("$.jobRecords", hasSize(7)));
  }

  @Test
  @DisplayName("Find jobs by source or desc")
  void findJobsBySourceOrDesc() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(source<>data-export-system-user or description==test-desc)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.totalRecords", is(7)),
              jsonPath("$.jobRecords", hasSize(7)));
  }

  @Test
  @DisplayName("Find jobs by date range")
  void findJobsByStrictDateRange() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(endTime>2020-12-12T00:00:00.000 and endTime<2020-12-13T23:59:59.999)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.totalRecords", is(0)));
  }

  @Test
  @DisplayName("Find jobs by attribute")
  void findJobsAttribute() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs?limit=30&offset=0&query=(metadata.endTime>2020-12-12T00:00:00.000)")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
              jsonPath("$.errors[0].message", startsWith("PathElementException")));
  }

  @Test
  @DisplayName("Fetch job by id")
  void getJob() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs/12ae5d0f-1525-44a1-a361-0bc9b88e8179")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.id", is("12ae5d0f-1525-44a1-a361-0bc9b88e8179")),
                jsonPath("$.status", is("SUCCESSFUL")),
                jsonPath("$.outputFormat", is("Fees & Fines Bursar Report")));
  }

  @Test
  @DisplayName("Should failed download file with NotFound")
  void shouldFailedDownloadWithNotFound() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs/35ae5d0f-1525-42a1-a361-1bc9b88e8180/download")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().is4xxClientError());
  }

  @Test
  @DisplayName("Should failed download file with BadRequest")
  void shouldFailedDownloadWithBadRequest() throws Exception {
    PresignedUrl presignedUrl = new PresignedUrl();
    presignedUrl.setUrl("http:/test-url/");
    when(exportWorkerClient.getRefreshedPresignedUrl(anyString())).thenReturn(presignedUrl);
    mockMvc
        .perform(
            get("/data-export-spring/jobs/42ae5d0f-6425-82a1-a361-1bc9b88e8172/download")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().is5xxServerError());
  }

  @Test
  @DisplayName("Can not fetch job with wrong id")
  void notFoundJob() throws Exception {
    mockMvc
        .perform(
            get("/data-export-spring/jobs/12ae5d0f-1525-44a1-a361-0bc9b88eeeee")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders()))
        .andExpectAll(
                status().isNotFound(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.errors[0].message", startsWith("NotFoundException")));
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
        .andExpectAll(
                status().isCreated(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.type", is("BURSAR_FEES_FINES")),
                jsonPath("$.status", is("SCHEDULED")),
                jsonPath("$.outputFormat", is("Fees & Fines Bursar Report")));
  }

  @Test
  @Disabled("The test fails because prior to Spring Batch v5 JobParameter value accepted null, but it does not anymore, so there should be change on logic which is not in scope of Spring batch migration")
  @DisplayName("Start new circulation export job")
  void postCirculationJob() throws Exception {
    mockMvc
        .perform(
            post("/data-export-spring/jobs")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(defaultHeaders())
                .content(JOB_CIRCULATION_REQUEST))
        .andExpectAll(
                status().isCreated(),
                content().contentType(MediaType.APPLICATION_JSON_VALUE),
                jsonPath("$.type", is("CIRCULATION_LOG")),
                jsonPath("$.status", is("SCHEDULED")),
                jsonPath("$.outputFormat", is("Comma-Separated Values (CSV)")));
  }

  @ParameterizedTest
  @MethodSource("getPayloadForJobWithoutRequiredParameters")
  @DisplayName("Start new job missing required parameters, should be 400")
  void shouldReturnBadRequestWhenRequiredParametersAreMissing(ExportType exportType,
                                                              ExportTypeSpecificParameters params) throws Exception {
    var payload = buildJobPayload(exportType, params);

    mockMvc.perform(post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(payload))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Start new authority control job with invalid dates, should be 400")
  void shouldReturnBadRequestWhenParametersInvalid() throws Exception {
    var date = LocalDate.now();
    var authorityControlExportConfig = new AuthorityControlExportConfig();
    authorityControlExportConfig.setToDate(date);
    authorityControlExportConfig.setFromDate(date.plusDays(1));

    var payload = buildAuthorityControlJobPayload(authorityControlExportConfig, ExportType.AUTH_HEADINGS_UPDATES);

    mockMvc.perform(post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(payload))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Start new auth headings update job, should be 201")
  void postAuthHeadingsUpdateJob() throws Exception {
    var date = LocalDate.now();
    var authorityControlExportConfig = new AuthorityControlExportConfig();
    authorityControlExportConfig.setFromDate(date);
    authorityControlExportConfig.setToDate(date.plusDays(1));

    var payload = buildAuthorityControlJobPayload(authorityControlExportConfig, ExportType.AUTH_HEADINGS_UPDATES);

    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(payload))
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Start new failed linked bib updates job, should be 201")
  void postFailedLinkedBibUpdatesJob() throws Exception {
    var payload = buildEmptyJobPayload(ExportType.FAILED_LINKED_BIB_UPDATES);

    mockMvc
      .perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(payload))
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Start new job with id filled, should be 200")
  void postJobWithIdShouldRespondOk() throws Exception {
    var job = new Job()
      .id(UUID.randomUUID())
      .type(ExportType.E_HOLDINGS)
      .exportTypeSpecificParameters(new ExportTypeSpecificParameters());
    var content =  MAPPER.writeValueAsString(job);

    mockMvc.perform(
        post("/data-export-spring/jobs")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders())
          .content(content))
      .andExpect(status().isOk());
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
      .andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON_VALUE),
          jsonPath("$.totalRecords", is(1)),
          jsonPath("$.jobRecords", hasSize(1)));
  }

  @Test
  @DisplayName("Test should Throw Exception If JSONBQuery Is Empty")
  void shouldThrowExceptionIfJSONBQueryIsEmpty() throws Exception {
    mockMvc
      .perform(
        get("/data-export-spring/jobs?limit=30&offset=0&query=jsonb==1 and type==\"EDIFACT_ORDERS_EXPORT\"")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .headers(defaultHeaders()))
      .andExpectAll(
          status().isBadRequest(),
          content().contentType(MediaType.APPLICATION_JSON_VALUE));
  }

  private static Stream<Arguments> getPayloadForJobWithoutRequiredParameters() {
    return Stream.of(
      Arguments.of(ExportType.E_HOLDINGS,
        new ExportTypeSpecificParameters().eHoldingsExportConfig(new EHoldingsExportConfig())),
      Arguments.of(ExportType.AUTH_HEADINGS_UPDATES,
        new ExportTypeSpecificParameters().authorityControlExportConfig(new AuthorityControlExportConfig())));
  }

  private String buildAuthorityControlJobPayload(AuthorityControlExportConfig authorityControlExportConfig,
                                                 ExportType exportType) throws JsonProcessingException {
    var exportTypeSpecificParameters = new ExportTypeSpecificParameters()
      .authorityControlExportConfig(authorityControlExportConfig);
    return buildJobPayload(exportType, exportTypeSpecificParameters);
  }

  private String buildEmptyJobPayload(ExportType exportType) throws JsonProcessingException {
    return buildJobPayload(exportType, new ExportTypeSpecificParameters());
  }

  private String buildJobPayload(ExportType exportType, ExportTypeSpecificParameters params)
    throws JsonProcessingException {
    var job = new Job()
      .type(exportType)
      .exportTypeSpecificParameters(params);

    return MAPPER.writeValueAsString(job);
  }
}
