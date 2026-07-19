package com.project.backend.app.audit.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.project.backend.app.audit.annotation.Auditable;
import com.project.backend.app.audit.services.AuditLogService;
import com.project.backend.app.security.auth.dto.SecurityUser;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @AfterReturning("@annotation(auditable)")
    public void audit(JoinPoint joinPoint, Auditable auditable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null)
            return;

        Object principal = auth.getPrincipal();
        if (!(principal instanceof SecurityUser user)) {
            return;
        }

        String target = auditable.target().isEmpty()
                ? joinPoint.getSignature().getName()
                : auditable.target();

        auditLogService.recordAuditLog(
                user.getUserId(),
                user.getTenantId(),
                auditable.action(),
                target,
                true
        );

    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void auditError(JoinPoint joinPoint, Auditable auditable, Exception ex) {

        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) return;

        Object principal = auth.getPrincipal();
        if (!(principal instanceof SecurityUser user)) return;

        String target = joinPoint.getSignature().getName();

        auditLogService.recordAuditLog(
            user.getUserId(),
            user.getTenantId(),
            auditable.action(),
            target,
            false
        );
    }

}
