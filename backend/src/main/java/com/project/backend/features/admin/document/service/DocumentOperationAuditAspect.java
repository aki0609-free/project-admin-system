package com.project.backend.features.admin.document.service;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.document.enums.DocumentArea;

@Aspect
@Component
public class DocumentOperationAuditAspect {

    private static final Logger LOG = LoggerFactory.getLogger(
            DocumentOperationAuditAspect.class
    );
    private static final Set<String> AUDITED_OPERATIONS = Set.of(
            "download",
            "createDirectory",
            "upload",
            "copy",
            "move",
            "rename",
            "delete"
    );

    @Around("execution(public * com.project.backend.features.admin.document.service.DocumentManagementService.*(..))")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        if (!AUDITED_OPERATIONS.contains(operation)) {
            return joinPoint.proceed();
        }

        AuditContext context = auditContext(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            LOG.info(
                    "document_operation result=success operation={} area={} path={} userId={} tenantId={}",
                    operation,
                    context.area(),
                    context.path(),
                    context.userId(),
                    context.tenantId()
            );
            return result;
        } catch (Throwable throwable) {
            LOG.warn(
                    "document_operation result=failure operation={} area={} path={} userId={} tenantId={} errorType={}",
                    operation,
                    context.area(),
                    context.path(),
                    context.userId(),
                    context.tenantId(),
                    throwable.getClass().getSimpleName()
            );
            throw throwable;
        }
    }

    private AuditContext auditContext(Object[] arguments) {
        String area = arguments.length > 0
                && arguments[0] instanceof DocumentArea documentArea
                ? documentArea.name()
                : "UNKNOWN";
        String path = buildPath(arguments);

        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        String userId = "SYSTEM";
        String tenantId = TenantContext.getTenantId();
        if (authentication != null
                && authentication.getPrincipal() instanceof SecurityUser user) {
            userId = String.valueOf(user.getUserId());
            tenantId = user.getTenantId();
        }
        return new AuditContext(
                area,
                sanitize(path),
                userId,
                sanitize(tenantId)
        );
    }

    private String buildPath(Object[] arguments) {
        StringBuilder result = new StringBuilder();
        for (int index = 1; index < arguments.length; index++) {
            Object argument = arguments[index];
            if (argument instanceof String value) {
                if (!result.isEmpty()) {
                    result.append(" -> ");
                }
                result.append(value);
            } else if (argument instanceof MultipartFile file) {
                if (!result.isEmpty()) {
                    result.append('/');
                }
                result.append(file.getOriginalFilename());
            }
        }
        return result.isEmpty() ? "-" : result.toString();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value
                .replace('\n', '_')
                .replace('\r', '_')
                .replace('\t', '_');
        return sanitized.length() <= 500
                ? sanitized
                : sanitized.substring(0, 500);
    }

    private record AuditContext(
            String area,
            String path,
            String userId,
            String tenantId
    ) {
    }
}
