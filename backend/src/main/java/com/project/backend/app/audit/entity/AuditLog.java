package com.project.backend.app.audit.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.mongodb.core.mapping.Document;

import com.project.backend.app.audit.enums.AuditAction;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collation = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private String id;

    private Long userId;

    private String tenantId;

    private AuditAction action;

    private String target;

    private String traceId;

    private boolean isSuccess;

    @CreationTimestamp
    private Instant timestamp;
    
}
