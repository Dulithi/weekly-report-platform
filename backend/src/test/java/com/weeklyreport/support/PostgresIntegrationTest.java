package com.weeklyreport.support;

import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Keep developer shell variables from inserting demo fixtures into ordinary
// integration tests. The dedicated seeder test overrides this property.
@TestPropertySource(properties = "app.demo-seed.enabled=false")
public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES;

    private static final String TEST_JWT_SECRET =
            Base64.getEncoder().encodeToString(
                    "01234567890123456789012345678901"
                            .getBytes()
            );

    static {
        POSTGRES = new PostgreSQLContainer("postgres:18.6");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", POSTGRES::getJdbcUrl);
        registry.add("DATABASE_USERNAME", POSTGRES::getUsername);
        registry.add("DATABASE_PASSWORD", POSTGRES::getPassword);
        
        registry.add("JWT_SECRET_BASE64",() -> TEST_JWT_SECRET);
        registry.add("REFRESH_COOKIE_SECURE",() -> "false");
        registry.add("REFRESH_COOKIE_SAME_SITE", () -> "Strict");
        registry.add("FRONTEND_URL", () -> "http://localhost:3000");
    }
}
