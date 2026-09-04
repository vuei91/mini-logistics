package com.cjlogistics.mini.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer bearerAuthCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (!path.startsWith("/auth/")) {
                pathItem.readOperations().forEach(operation ->
                        operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth")));
            }
        });
    }

    @Bean
    public OpenAPI cjLogisticsMiniOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                .title("CJ Logistics Mini API")
                .description("화주-차주 매칭 및 배차 시스템 (더운반 벤치마크 미니 포트폴리오)")
                .version("0.0.1")
                .contact(new Contact().name("Portfolio").url("https://github.com/"))
                .license(new License().name("MIT")))
                .components(new io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
