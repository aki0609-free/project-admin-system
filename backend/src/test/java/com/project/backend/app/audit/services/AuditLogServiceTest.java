package com.project.backend.app.audit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import com.project.backend.app.audit.entity.AuditLog;
import com.project.backend.app.audit.enums.AuditAction;
import com.project.backend.app.audit.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditLogService auditLogService;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordLogout_shouldPersistLogoutContextAndTraceId() {
        MDC.put("traceId", "trace-logout-001");

        auditLogService.recordLogout(
                42L,
                "default",
                "MANUAL",
                "0123456789abcdef",
                "Test Browser",
                true
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(
                AuditLog.class
        );
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getTenantId()).isEqualTo("default");
        assertThat(saved.getAction()).isEqualTo(AuditAction.LOGOUT);
        assertThat(saved.getLogoutReason()).isEqualTo("MANUAL");
        assertThat(saved.getSessionId()).isEqualTo("0123456789abcdef");
        assertThat(saved.getDeviceInfo()).isEqualTo("Test Browser");
        assertThat(saved.getTraceId()).isEqualTo("trace-logout-001");
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getTimestamp()).isNotNull();
    }
}
