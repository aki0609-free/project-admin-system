package com.project.backend.app.tenant.filter;

import java.io.IOException;

import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.backend.app.tenant.context.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Session session = entityManager.unwrap(Session.class);
        String tenantId = request.getHeader("X-Tenant-ID");

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);

            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", tenantId);
            
            session.enableFilter("softDeleteFilter");
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            session.disableFilter("tenantFilter");
            session.disableFilter("softDeleteFilter");
        }
    }

}
