package com.project.backend.app.base.entity;

import java.time.Instant;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import com.project.backend.app.tenant.context.TenantContext;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = String.class)
)
@Filter(
    name = "tenantFilter",
    condition = "tenant_id = :tenantId"
)
@FilterDef(
    name = "softDeleteFilter"
)
@Filter(
    name = "softDeleteFilter",
    condition = "deleted_at IS NULL"
)
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();

        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();

        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
    }
    
}
