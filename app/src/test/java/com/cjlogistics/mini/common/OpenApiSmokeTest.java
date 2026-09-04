package com.cjlogistics.mini.common;

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
    void api_docs_endpoint_returns_200_and_lists_shippers() {
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
    }

    @Test
    void swagger_ui_html_is_reachable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
