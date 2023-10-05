package org.folio.des;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.TestSocketUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


/**
 * Test that shaded fat uber jar and Dockerfile work.
 *
 * <p>Smoke tests: /admin/health and migration.
 */
@Testcontainers
@DirtiesContext
class InstallUpgradeIT {

  private static final Logger LOG = LoggerFactory.getLogger(InstallUpgradeIT.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
    .parse("mockserver/mockserver")
    .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
  private static final Network NETWORK = Network.newNetwork();
  private static final String EDIFACT_CONFIG_QUERY = "module==mod-data-export-spring and value==*EDIFACT_ORDERS_EXPORT*";
  private static MockServerClient mockServerClient;

  @Container
  public static final KafkaContainer KAFKA =
    new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.5.3"))
    .withNetwork(NETWORK)
    .withNetworkAliases("mykafka");

  @Container
  public static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>("postgres:12-alpine")
    .withClasspathResourceMapping("v1.2.3.sql", "/v1.2.3.sql", BindMode.READ_ONLY)
    .withNetwork(NETWORK)
    .withNetworkAliases("mypostgres")
    .withExposedPorts(5432)
    .withUsername("username")
    .withPassword("password")
    .withDatabaseName("postgres");

  @Container
  private static final MockServerContainer OKAPI =
    new MockServerContainer(MOCKSERVER_IMAGE)
    .withNetwork(NETWORK)
    .withNetworkAliases("okapi")
    .withExposedPorts(1080);

  @Container
  public static final GenericContainer<?> MOD_DES =
    new GenericContainer<>(
      new ImageFromDockerfile("mod-data-export-spring").withFileFromPath(".", Path.of(".")))
    .withNetwork(NETWORK)
    .withExposedPorts(8081)
    .withEnv("DB_HOST", "mypostgres")
    .withEnv("DB_PORT", "5432")
    .withEnv("DB_USERNAME", "username")
    .withEnv("DB_PASSWORD", "password")
    .withEnv("DB_DATABASE", "postgres")
    .withEnv("KAFKA_HOST", "mykafka")
    .withEnv("KAFKA_PORT", "9092")
    .withEnv("SYSTEM_USER_PASSWORD", "password");

  private static void mockPath(MockServerClient mockServerClient, String path, String jsonBody) {
    mockServerClient.when(request(path))
    .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON).withBody(jsonBody));
  }

  @BeforeAll
  static void beforeClass() {
    RestAssured.reset();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

    RestAssured.baseURI = "http://" + MOD_DES.getHost() + ":" + MOD_DES.getFirstMappedPort();

    MOD_DES.followOutput(new Slf4jLogConsumer(LOG).withSeparateOutputStreams());

    mockServerClient = new MockServerClient(OKAPI.getHost(), OKAPI.getServerPort());
  }

  @BeforeEach
  void beforeEach() {
    RestAssured.requestSpecification = null;

    mockServerClient.when(request("/users").withMethod("PUT")).respond(response().withStatusCode(201));
    mockPath(mockServerClient, "/users.*",       "{\"users\": []}");
    mockPath(mockServerClient, "/perms/users.*", "{\"permissionUsers\": []}");
    mockServerClient.when(request().withPath("/configurations/entries")
        .withQueryStringParameter("query", EDIFACT_CONFIG_QUERY).withMethod("GET"))
      .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
        .withBody(CONFIGS_RESPONSE));
    mockServerClient.when(request())
      .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
        .withBody("{\"totalRecords\": 0}"));
  }

  @AfterEach
  void afterEach(){
    mockServerClient.reset();
  }

  private void setTenant(String tenant) {
    RestAssured.requestSpecification = new RequestSpecBuilder()
        .addHeader("X-Okapi-Url", "http://okapi:1080")
        .addHeader("X-Okapi-Tenant", tenant)
        .setContentType(ContentType.JSON)
        .build();
  }

  @Test
  void health() {
    when().
      get("/admin/health").
    then().
      statusCode(200).
      body("status", is("UP"));
  }

  private void postTenant(ObjectNode body) {
    given().
      body(body.toPrettyString()).
    when().
      post("/_/tenant").
    then().
      statusCode(204);
  }

  private void smokeTest() {
    when().
      get("/data-export-spring/jobs").
    then().
      statusCode(200).
      body("totalRecords", is(0));
  }

  @Test
  void installAndUpgrade() {
    setTenant("latest");
    postTenant(createObjectNode().put("module_to", "999999.0.0"));
    // migrate from 0.0.0, migration should be idempotent
    postTenant(createObjectNode().put("module_to", "999999.0.0").put("module_from", "0.0.0"));

    smokeTest();
  }

  @Test
  void upgradeFromKiwi() {
    // load database dump of Kiwi R2 2021 version of mod-data-export-spring
    postgresExec("psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
        "-f", "v1.2.3.sql");

    setTenant("kiwi");

    // migrate
    postTenant(createObjectNode().put("module_to", "999999.0.0").put("module_from", "1.2.3"));

    smokeTest();
  }

  @Test
  void upgradeToPoppy() {
    //upgrade from orchid to poppy for which quartz scheduling was enabled
    setTenant("latest");
    postTenant(createObjectNode().put("module_to", "3.0.0").put("module_from", "2.1.0"));
    //existing schedule configurations should be loaded to be scheduled with quartz
    verifySchedulingConfigsReloaded();
    smokeTest();
  }

  @Test
  void upgradeFromPoppy() {
    //upgrade from poppy for which quartz scheduling was enabled to newer release
    setTenant("latest");
    postTenant(createObjectNode().put("module_to", "3.1.0").put("module_from", "3.0.0"));
    // module_from supports quartz scheduling already, no need to reload scheduling configs
    verifySchedulingConfigsNotReloaded();
    smokeTest();
  }

  @Test
  void upgradeFromPoppyWithForcedSchedulesReload() {
    //upgrade from poppy for which quartz scheduling was enabled to newer release
    setTenant("latest");
    ObjectNode request = createObjectNode().put("module_to", "3.1.0").put("module_from", "3.0.0");
    request.putArray("parameters").add(createObjectNode().put("key", "forceSchedulesReload")
      .put("value", "true"));
    postTenant(request);
    //forceSchedulesReload is set to true, so scheduling configs need to be reloaded
    verifySchedulingConfigsReloaded();
    smokeTest();
  }

  static ObjectNode createObjectNode() {
    return OBJECT_MAPPER.createObjectNode();
  }

  static void postgresExec(String... command) {
    try {
      ExecResult execResult = POSTGRES.execInContainer(command);
      LOG.info(String.join(" ", command) + " " + execResult);
    } catch (InterruptedException | IOException | UnsupportedOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static void verifySchedulingConfigsReloaded() {
    verifySchedulingConfigReload(1);
  }

  private static void verifySchedulingConfigsNotReloaded() {
    verifySchedulingConfigReload(0);
  }

  private static void verifySchedulingConfigReload(int times) {
    mockServerClient.verify(request()
      .withPath("/configurations/entries")
      .withMethod("GET")
      .withQueryStringParameter("query", "module==mod-data-export-spring and value==*EDIFACT_ORDERS_EXPORT*"), VerificationTimes.exactly(times));
  }

  private static final String CONFIGS_RESPONSE= """
    {
    	"configs": [
    		{
    			"id": "9fa30f7a-0de4-4067-864a-5449d85b1d65",
    			"value": "{\\"id\\":\\"5a3cba28-16e7-498e-b73b-98713000298e\\",  \\"tenant\\": \\"some_tenant\\", \\"type\\": \\"EDIFACT_ORDERS_EXPORT\\", \\"exportTypeSpecificParameters\\": { \\"vendorEdiOrdersExportConfig\\": {\\"vendorId\\": \\"046b6c7f-0b8a-43b9-b35d-6489e6daee91\\", \\"configName\\": \\"edi_config\\", \\"ediSchedule\\": {\\"enableScheduledExport\\": true, \\"scheduleParameters\\": {\\"scheduleFrequency\\": 1, \\"schedulePeriod\\": \\"WEEK\\", \\"weekDays\\":[\\"SUNDAY\\"], \\"scheduleTime\\": \\"15:30:00\\"}}}}, \\"schedulePeriod\\": \\"HOUR\\"}",
    			"module": "mod-data-export-spring",
    			"default": true,
    			"enabled": true,
    			"metadata": {
    				"createdDate": "2023-04-30T12:56:26.211Z",
    				"updatedDate": "2023-05-04T21:41:53.887Z",
    				"createdByUserId": "850e9737-1b60-5553-9423-a6f6251aeb0b",
    				"updatedByUserId": "850e9737-1b60-5553-9423-a6f6251aeb0b"
    			},
    			"configName": "EDIFACT_ORDERS_EXPORT_e0fb5df2-cdf1-11e8-a8d5-f2801f1b9fd1_9fa30f7a-0de4-4067-864a-5449d85b1d65",
    			"description": "Edifact orders export configuration parameters"
    		}
    	],
    	"totalRecords": 1
    }
    """;
}
