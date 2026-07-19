package com.project.backend.app.persistence;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.project.backend.app.tenant.context.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class HibernateFilterEnabler {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableUserFilters() {
        Session session = entityManager.unwrap(Session.class);

        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("TenantContext に tenantId が設定されていません。");
        }

        session.enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);

        session.enableFilter("softDeleteFilter");
    }
}