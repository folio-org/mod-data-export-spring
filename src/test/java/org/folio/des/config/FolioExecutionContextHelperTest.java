package org.folio.des.config;

import org.folio.des.support.BaseTest;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class FolioExecutionContextHelperTest extends BaseTest {

  @Autowired
  private FolioExecutionContextHelper contextHelper;

  private static final String SYSTEM_USER = """
    {
      "users": [
        {
          "username": "data-export-system-user",
          "id": "a85c45b7-d427-4122-8532-5570219c5e59",
          "active": true,
          "departments": [],
          "proxyFor": [],
          "personal": {
            "addresses": []
          },
          "createdDate": "2021-03-17T15:30:07.106+00:00",
          "updatedDate": "2021-03-17T15:30:07.106+00:00",
          "metadata": {
            "createdDate": "2021-03-17T15:21:26.064+00:00",
            "updatedDate": "2021-03-17T15:30:07.043+00:00"
          }
        }
      ],
      "totalRecords": 1,
      "resultInfo": {
        "totalRecords": 1,
        "facets": [],
        "diagnostics": []
      }
    }
    """;

  @Test
  void shouldGetFolioExecutionContextHelper() {
    // request to get token for 'data-export-system-user'
    wireMockServer.stubFor(
      post(urlEqualTo("/authn/login"))
        .willReturn(aResponse()
          .withHeader(XOkapiHeaders.TOKEN, TOKEN)));

    // request to get list of users by 'username' (='data-export-system-user')
    wireMockServer.stubFor(
      get(urlEqualTo("/users?query=username%3D%3Ddata-export-system-user"))
        .willReturn(aResponse()
          .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
          .withBody(SYSTEM_USER)));

    // 'execution context' should be created according to 'data-export-system-user' headers
    FolioExecutionContext executionContext = contextHelper.getFolioExecutionContext(TENANT);

    Assertions.assertEquals(executionContext.getTenantId(), TENANT);
    Assertions.assertEquals(executionContext.getOkapiUrl(), wireMockServer.baseUrl());
    Assertions.assertEquals(executionContext.getToken(), TOKEN);
    Assertions.assertEquals(executionContext.getUserId().toString(), "a85c45b7-d427-4122-8532-5570219c5e59");
  }

  @Test
  void shouldGetExceptionWhenThereIsNoToken() {
    // request to get response without 'XOkapiHeaders.TOKEN'
    wireMockServer.stubFor(
      post(urlEqualTo("/authn/login"))
        .willReturn(aResponse()));

    // should get exception because of missing token
    Assertions.assertThrows(IllegalStateException.class, () -> contextHelper.getFolioExecutionContext(TENANT));
  }
}
