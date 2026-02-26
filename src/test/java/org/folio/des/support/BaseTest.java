package org.folio.des.support;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.tomakehurst.wiremock.WireMockServer;

import lombok.SneakyThrows;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@ContextConfiguration(initializers = BaseTest.DockerPostgreDataSourceInitializer.class)
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(topics = { "diku.data-export.job.update" })
@EnableKafka
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

  public static final int WIRE_MOCK_PORT = TestSocketUtils.findAvailableTcpPort();
  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  public static final String TENANT = "diku";
  public static final String USER_ID = "625dd2b6-b6f2-4f77-90fe-68954b26ee3c";

  public static WireMockServer wireMockServer;
  public static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected Scheduler scheduler;
  @Autowired
  private FolioEnvironment folioEnvironment;


  static {
    postgreDBContainer.start();
  }

  public static class DockerPostgreDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
          "spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
          "spring.datasource.username=" + postgreDBContainer.getUsername(),
          "spring.datasource.password=" + postgreDBContainer.getPassword());
    }
  }

  @BeforeAll
  void beforeAll(@Autowired MockMvc mockMvc) {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();
    folioEnvironment.setOkapiUrl(wireMockServer.baseUrl());
    setUpTenant(mockMvc);
  }

  @BeforeEach
  void beforeEach() throws SchedulerException {
    scheduler.clear();
  }

  @SneakyThrows
  protected static void setUpTenant(MockMvc mockMvc) {
    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().moduleTo("mod-data-export-spring-3.0.0")))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(Include.NON_NULL))
      .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(Include.NON_NULL))
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true).build();

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    httpHeaders.add(XOkapiHeaders.URL, wireMockServer.baseUrl());
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);

    return httpHeaders;
  }

  @AfterEach
  void afterEach() throws SchedulerException {
    if (!scheduler.isInStandbyMode()) {
      scheduler.clear();
    }
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }

  @DynamicPropertySource
  static void setFolioOkapiUrl(DynamicPropertyRegistry registry) {
    registry.add("folio.okapi.url", () -> "http://localhost:" + WIRE_MOCK_PORT);
  }
}
