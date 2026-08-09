package com.project.backend.testsupport;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final Instant DEFAULT_TEST_INSTANT =
            Instant.parse("2026-01-01T00:00:00Z");
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Tokyo");

    @Bean
    @Primary
    AdjustableTestClock adjustableTestClock() {
        return new AdjustableTestClock(DEFAULT_TEST_INSTANT, BUSINESS_ZONE);
    }

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0.46")
                .withDatabaseName("ADMIN")
                .withUsername("projectadmin_test")
                .withPassword("projectadmin_test");
    }

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer("mongo:8.0");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7.4-alpine")
                .withExposedPorts(6379);
    }
}
