package com.project.backend.app.audit.services;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.project.backend.app.audit.entity.AuditLog;
import com.project.backend.app.audit.enums.AuditAction;
import com.project.backend.app.audit.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AuditLogService.class
    );

    private final AuditLogRepository repository;

    public void recordAuditLog(
        Long userId,
        String tenantId,
        AuditAction action,
        String target,
        boolean isSuccess
    ) {
        String traceId = MDC.get("traceId");

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .tenantId(tenantId)
                .action(action)
                .target(target)
                .traceId(traceId)
                .isSuccess(isSuccess)
                .timestamp(Instant.now())
                .build();

        if (log != null) {
            repository.save(log);
        }
    }

    public void recordLogout(
            Long userId,
            String tenantId,
            String logoutReason,
            String sessionId,
            String deviceInfo,
            boolean isSuccess
    ) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .tenantId(tenantId)
                .action(AuditAction.LOGOUT)
                .target("AUTH_SESSION")
                .traceId(MDC.get("traceId"))
                .logoutReason(logoutReason)
                .sessionId(sessionId)
                .deviceInfo(deviceInfo)
                .isSuccess(isSuccess)
                .timestamp(Instant.now())
                .build();

        try {
            repository.save(auditLog);
        } catch (RuntimeException exception) {
            // セッション失効自体は監査DBの一時障害で妨げない。
            LOGGER.error(
                    "Failed to persist logout audit. sessionId={}, traceId={}",
                    sessionId,
                    auditLog.getTraceId(),
                    exception
            );
        }
    }
    
}
