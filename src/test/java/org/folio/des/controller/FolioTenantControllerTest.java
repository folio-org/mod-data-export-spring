package org.folio.des.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.Test;

class FolioTenantControllerTest extends BaseTest {

  @Test
  void shouldPostDefaultBulkEditConfigurationUponPostTenant() {
    var expectedBody = "{" +
      "  \"module\" : \"BULKEDIT\"," +
      "  \"configName\" : \"general\"," +
      "  \"default\" : true," +
      "  \"enabled\" : true," +
      "  \"value\" : \"{\\\"jobExpirationPeriod\\\":\\\"14\\\"}\"" +
      "}";

    wireMockServer.verify(
      postRequestedFor(
        urlEqualTo("/configurations/entries"))
        .withRequestBody(equalToJson(expectedBody)));
  }
}
