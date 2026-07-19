package com.project.backend.app.audit.services;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.project.backend.app.audit.entity.AuditLog;
import com.project.backend.app.audit.enums.AuditAction;
import com.project.backend.app.audit.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

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
                .build();

        if (log != null) {
            repository.save(log);
        }
    }
    
}
