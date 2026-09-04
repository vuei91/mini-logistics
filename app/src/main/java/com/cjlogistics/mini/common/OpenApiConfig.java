package com.cjlogistics.mini.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer authorizationHeaderParameterCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    boolean requiresBearerAuth = operation.getSecurity() != null
                            && operation.getSecurity().stream()
                            .anyMatch(requirement -> requirement.containsKey("BearerAuth"));
                    if (requiresBearerAuth) {
                        operation.addParametersItem(new Parameter()
                                .in("header")
                                .name("Authorization")
                                .required(true)
                                .description("Bearer {accessToken} 형식으로 로그인 토큰을 입력하세요.")
                                .schema(new StringSchema().example("Bearer {accessToken}")));
                    }
                })
        );
    }

    @Bean
    public OpenAPI cjLogisticsMiniOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("BearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .info(new Info()
                .title("CJ Logistics Mini API")
                .description("화주-차주 매칭 및 배차 시스템 (더운반 벤치마크 미니 포트폴리오)")
                .version("0.0.1")
                .contact(new Contact().name("Portfolio").url("https://github.com/"))
                .license(new License().name("MIT")));
    }
}
