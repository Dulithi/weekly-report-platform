package com.weeklyreport.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.weeklyreport.security.SecurityProperties;
import com.weeklyreport.security.CurrentAccountAuthoritiesConverter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CookieCsrfTokenRepository csrfTokenRepository
    ) throws Exception {

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // Auth endpoints accept/set cookies; other APIs require explicit bearer tokens.
                        .requireCsrfProtectionMatcher(new AndRequestMatcher(
                                CsrfFilter.DEFAULT_CSRF_MATCHER,
                                PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/**")
                        ))
                )

                .cors(Customizer.withDefaults())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout"
                        ).permitAll()

                        .requestMatchers(
                                "/api/v1/admin/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/api/v1/manager/**"
                        ).hasAnyRole(
                                "MANAGER",
                                "ADMIN"
                        )
                        .requestMatchers(
                                "/api/v1/reports/**"
                        ).hasRole("TEAM_MEMBER")

                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                .build();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(SecurityProperties properties) {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        boolean secure = properties.refreshToken().secure();
        // The production prefix prevents sibling domains from injecting this cookie.
        repository.setCookieName(secure ? "__Host-XSRF-TOKEN" : "XSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie
                .httpOnly(true)
                .secure(secure)
                .sameSite(properties.refreshToken().sameSite()));
        return repository;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(
            CurrentAccountAuthoritiesConverter authorities
    ) {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authorities);

        return converter;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            SecurityProperties properties
    ) {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        properties.cors().allowedOrigin()
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-XSRF-TOKEN"
                )
        );

        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Retry-After"));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
