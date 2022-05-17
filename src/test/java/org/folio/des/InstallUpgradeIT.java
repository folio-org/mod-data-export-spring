package org.folio.des;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


/**
 * Test that shaded fat uber jar and Dockerfile work.
 *
 * <p>Smoke tests: /admin/health and migration.
 */
@Testcontainers
public class InstallUpgradeIT {

  private static final Logger LOG = LoggerFactory.getLogger(InstallUpgradeIT.class);
  private static final Network NETWORK = Network.newNetwork();

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
    new MockServerContainer(DockerImageName.parse("mockserver/mockserver:mockserver-5.13.2"))
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
    .withEnv("KAFKA_PORT", "9092");

  private static void mockPath(MockServerClient mockServerClient, String path, String jsonBody) {
    mockServerClient.when(request(path))
    .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON).withBody(jsonBody));
  }

  @BeforeAll
  static void beforeClass() {
    RestAssured.reset();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + MOD_DES.getHost() + ":" + MOD_DES.getFirstMappedPort();

    MOD_DES.followOutput(new Slf4jLogConsumer(LOG).withSeparateOutputStreams());

    var mockServerClient = new MockServerClient(OKAPI.getHost(), OKAPI.getServerPort());
    mockServerClient.when(request("/users").withMethod("PUT")).respond(response().withStatusCode(201));
    mockPath(mockServerClient, "/users.*",       "{\"users\": []}");
    mockPath(mockServerClient, "/perms/users.*", "{\"permissionUsers\": []}");
    mockServerClient.when(request())
        .respond(response().withStatusCode(200).withContentType(MediaType.APPLICATION_JSON)
            .withBody("{\"totalRecords\": 0}"));
  }

  @BeforeEach
  void beforeEach() {
    RestAssured.requestSpecification = null;
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

  private void postTenant(JsonObject body) {
    given().
      body(body.encodePrettily()).
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
    postTenant(new JsonObject().put("module_to", "999999.0.0"));
    // migrate from 0.0.0, migration should be idempotent
    postTenant(new JsonObject().put("module_to", "999999.0.0").put("module_from", "0.0.0"));

    smokeTest();
  }

  @Test
  void upgradeFromKiwi() {
    // load database dump of Kiwi R2 2021 version of mod-data-export-spring
    postgresExec("psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
        "-f", "v1.2.3.sql");

    setTenant("kiwi");

    // migrate
    postTenant(new JsonObject().put("module_to", "999999.0.0").put("module_from", "1.2.3"));

    smokeTest();
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

