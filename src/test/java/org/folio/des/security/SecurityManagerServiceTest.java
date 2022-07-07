package org.folio.des.security;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.folio.des.config.FolioExecutionContextHelper;
import org.folio.des.support.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class SecurityManagerServiceTest extends BaseTest {

  @Autowired private SecurityManagerService securityManagerService;
  @Autowired private FolioExecutionContextHelper contextHelper;
  private static final String SYS_USER_EXIST_RESPONSE =
      "{\n"
          + "    \"users\": [\n"
          + "        {\n"
          + "            \"username\": \"data-export-system-user\",\n"
          + "            \"id\": \"a85c45b7-d427-4122-8532-5570219c5e59\",\n"
          + "            \"active\": true,\n"
          + "            \"departments\": [],\n"
          + "            \"proxyFor\": [],\n"
          + "            \"personal\": {\n"
          + "                \"addresses\": []\n"
          + "            },\n"
          + "            \"createdDate\": \"2021-03-17T15:30:07.106+00:00\",\n"
          + "            \"updatedDate\": \"2021-03-17T15:30:07.106+00:00\",\n"
          + "            \"metadata\": {\n"
          + "                \"createdDate\": \"2021-03-17T15:21:26.064+00:00\",\n"
          + "                \"updatedDate\": \"2021-03-17T15:30:07.043+00:00\"\n"
          + "            }\n"
          + "        }\n"
          + "    ],\n"
          + "    \"totalRecords\": 1,\n"
          + "    \"resultInfo\": {\n"
          + "        \"totalRecords\": 1,\n"
          + "        \"facets\": [],\n"
          + "        \"diagnostics\": []\n"
          + "    }\n"
          + "}";

  private static final String USER_PERMS_RESPONSE =
      "{  \"permissionUsers\": [],\n  \"totalRecords\": 0,\n  \"resultInfo\": {\n    \"totalRecords\": 0,\n    \"facets\": [],\n    \"diagnostics\": []\n  }\n}";

  @BeforeEach
  void setUp() {
    contextHelper.initScope(TENANT);
  }

  @Test
  @DisplayName("Update user")
  void prepareSystemUser() {

    wireMockServer.stubFor(
        get(urlEqualTo("/users?query=username%3D%3Ddata-export-system-user"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(SYS_USER_EXIST_RESPONSE)));

    wireMockServer.stubFor(
        get(urlEqualTo("/perms/users?query=userId%3D%3Da85c45b7-d427-4122-8532-5570219c5e59"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(USER_PERMS_RESPONSE)));

    securityManagerService.prepareSystemUser(wireMockServer.baseUrl(), TENANT);

    wireMockServer.verify(
        getRequestedFor(urlEqualTo("/users?query=username%3D%3Ddata-export-system-user")));
    wireMockServer.verify(
        putRequestedFor(urlEqualTo("/users/a85c45b7-d427-4122-8532-5570219c5e59")));
  }
}
