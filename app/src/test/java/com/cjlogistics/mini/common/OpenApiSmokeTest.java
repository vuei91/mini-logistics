package com.cjlogistics.mini.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OpenApiSmokeTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void api_docs_endpoint_returns_200_and_marks_protected_operations_with_bearer_auth() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // 등록된 컨트롤러가 스펙에 노출되는지 확인
        assertThat(response.getBody()).contains("/shippers");
        assertThat(response.getBody()).contains("/drivers");
        assertThat(response.getBody()).contains("/shipment-requests");
        assertThat(response.getBody()).contains("/dispatches");
        // 프로젝트 메타데이터 노출
        assertThat(response.getBody()).contains("CJ Logistics Mini API");
        assertThat(response.getBody()).contains("BearerAuth");
        assertThat(response.getBody()).contains("bearer");
        assertThat(response.getBody()).contains("/auth/shippers/signup");
        assertThat(response.getBody()).contains("/auth/drivers/signup");
        assertThat(response.getBody()).contains("/auth/shippers/login");
        assertThat(response.getBody()).contains("/auth/drivers/login");
        JsonNode openApi = new ObjectMapper().readTree(response.getBody());
        // 로그인/회원가입은 공개 API이고, 업무 API는 각 operation에 JWT 요구사항을 명시한다.
        assertThat(openApi.path("security").isMissingNode()).isTrue();
        assertThat(openApi.path("paths").path("/shipment-requests").path("post")
                .path("security").get(0).has("BearerAuth")).isTrue();
        assertThat(openApi.path("paths").path("/shipment-requests").path("post")
                .path("parameters").toString()).contains("\"name\":\"Authorization\"");
        assertThat(openApi.path("paths").path("/auth/shippers/login").path("post")
                .path("security").isMissingNode()).isTrue();
    }

    @Test
    void swagger_ui_html_is_reachable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
