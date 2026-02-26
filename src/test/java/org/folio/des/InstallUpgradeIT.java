package org.folio.des;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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
  private static MockServerClient mockServerClient;

  @Container
  public static final KafkaContainer KAFKA =
    new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
    .withNetwork(NETWORK)
    .withListener("mykafka:19092")
    .withStartupAttempts(3);

  @Container
  public static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>("postgres:16-alpine")
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
    .withEnv("KAFKA_PORT", "19092")
    .withEnv("SYSTEM_USER_ENABLED", "false");

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
    smokeTest();
  }

  @Test
  void upgradeFromPoppy() {
    //upgrade from poppy for which quartz scheduling was enabled to newer release
    setTenant("latest");
    postTenant(createObjectNode().put("module_to", "3.1.0").put("module_from", "3.0.0"));
    // module_from supports quartz scheduling already, no need to reload scheduling configs
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

}
