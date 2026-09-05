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
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    String securityScheme = roleFor(path, method.name());
                    if (securityScheme != null) {
                        operation.addSecurityItem(new SecurityRequirement().addList(securityScheme));
                    } else {
                        operation.addSecurityItem(new SecurityRequirement().addList("shipperAuth"));
                        operation.addSecurityItem(new SecurityRequirement().addList("driverAuth"));
                    }
                });
            }
        });
    }

    private String roleFor(String path, String method) {
        if ("POST".equals(method) && "/shipment-requests".equals(path)) {
            return "shipperAuth";
        }
        if ("POST".equals(method) && path.matches("/shipment-requests/[^/]+/cancel")) {
            return "shipperAuth";
        }
        if ("POST".equals(method) && path.matches("/shipment-requests/[^/]+/dispatch")) {
            return "shipperAuth";
        }
        if ("POST".equals(method) && path.matches("/dispatches/[^/]+/(accept|reject)")) {
            return "driverAuth";
        }
        if ("PATCH".equals(method) && path.matches("/dispatches/[^/]+/status")) {
            return "driverAuth";
        }
        return null;
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
                    .addSecuritySchemes("shipperAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                    .addSecuritySchemes("driverAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
