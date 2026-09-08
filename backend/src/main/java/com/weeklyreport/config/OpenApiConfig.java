package com.weeklyreport.config;

import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";
    static final String CSRF_SCHEME = "csrfToken";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/csrf",
            "/api/v1/auth/register",
            "/api/v1/auth/invitation-acceptances",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    @Bean
    OpenAPI weeklyReportOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weekly Report API")
                        .description("REST API for weekly reports, reviews, projects and team administration.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes(CSRF_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")
                                .description("Masked token returned by GET /api/v1/auth/csrf. "
                                        + "Call that endpoint first so the matching CSRF cookie is also set.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /**
     * The API uses bearer authentication by default. Authentication bootstrap
     * endpoints are public, while their state-changing requests require CSRF
     * proof. Override the inherited bearer requirement so the contract matches
     * the actual security filter chain.
     */
    @Bean
    OpenApiCustomizer publicAuthenticationOperations() {
        return openApi -> PUBLIC_PATHS.forEach(path -> {
            var pathItem = openApi.getPaths().get(path);
            if (pathItem != null) {
                pathItem.readOperations().forEach(operation -> operation.setSecurity(
                        path.equals("/api/v1/auth/csrf")
                                ? java.util.List.of()
                                : java.util.List.of(new SecurityRequirement().addList(CSRF_SCHEME))
                ));
            }
        });
    }
}
