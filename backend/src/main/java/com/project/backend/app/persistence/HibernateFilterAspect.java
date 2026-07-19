package com.project.backend.app.persistence;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.project.backend.app.tenant.context.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import static net.logstash.logback.argument.StructuredArguments.*;

@Aspect
@Component
@Slf4j
public class HibernateFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@within(com.project.backend.app.persistence.EnableHibernateFilters) || " +
            "@annotation(com.project.backend.app.persistence.EnableHibernateFilters)")
    public void enableFilters() {
        Session session = entityManager.unwrap(Session.class);

        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("TenantContext に tenantId が設定されていません。");
        }

        Filter tenantFilter = session.getEnabledFilter("tenantFilter");
        if (tenantFilter == null) {
            session.enableFilter("tenantFilter")
                    .setParameter("tenantId", tenantId);
        } else {
            tenantFilter.setParameter("tenantId", tenantId);
        }

        if (session.getEnabledFilter("softDeleteFilter") == null) {
            session.enableFilter("softDeleteFilter");
        }

        log.debug(
                "hibernate filter enabled",
                keyValue("event", "HIBERNATE_FILTER"),
                keyValue("tenantId", tenantId),
                keyValue("filters", "tenantFilter,softDeleteFilter")
            );
    }
}