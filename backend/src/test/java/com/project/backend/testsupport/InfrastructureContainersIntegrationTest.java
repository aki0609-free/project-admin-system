package com.project.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.project.backend.app.audit.entity.AuditLog;
import com.project.backend.app.audit.enums.AuditAction;
import com.project.backend.app.audit.repository.AuditLogRepository;

class InfrastructureContainersIntegrationTest
        extends ContainerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void mysqlContainerAcceptsApplicationQueries() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );
        String databaseName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        assertThat(result).isEqualTo(1);
        assertThat(databaseName).isEqualToIgnoringCase("ADMIN");
    }

    @Test
    void redisContainerSupportsReadWriteAndDelete() {
        String key = "integration:test:" + UUID.randomUUID();

        try {
            redisTemplate.opsForValue().set(key, "connected");

            assertThat(redisTemplate.opsForValue().get(key))
                    .isEqualTo("connected");
        } finally {
            redisTemplate.delete(key);
        }
    }

    @Test
    void mongoContainerPersistsAuditDocument() {
        AuditLog auditLog = AuditLog.builder()
                .userId(1L)
                .tenantId("integration-test")
                .action(AuditAction.CREATE_USER)
                .target("testcontainers")
                .traceId(UUID.randomUUID().toString())
                .isSuccess(true)
                .timestamp(Instant.now())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);

        try {
            assertThat(saved.getId()).isNotBlank();
            assertThat(auditLogRepository.findById(saved.getId()))
                    .isPresent()
                    .get()
                    .extracting(AuditLog::getTenantId)
                    .isEqualTo("integration-test");
        } finally {
            auditLogRepository.deleteById(saved.getId());
        }
    }
}
