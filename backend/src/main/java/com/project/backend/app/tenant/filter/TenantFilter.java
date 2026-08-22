package com.project.backend.app.tenant.filter;

import java.io.IOException;
import java.util.regex.Pattern;

import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.tenant.context.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Pattern TENANT_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");

    private final EntityManager entityManager;

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestedTenantId;
        try {
            requestedTenantId = normalizeTenantId(
                    request.getHeader(TENANT_HEADER)
            );
        } catch (IllegalArgumentException exception) {
            reject(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage());
            return;
        }

        String authenticatedTenantId;
        try {
            authenticatedTenantId = authenticatedTenantId();
        } catch (IllegalArgumentException exception) {
            reject(response, HttpServletResponse.SC_FORBIDDEN,
                    "認証情報のTenant IDが不正です。");
            return;
        }

        if (authenticatedTenantId != null) {
            if (requestedTenantId != null
                    && !authenticatedTenantId.equals(requestedTenantId)) {
                reject(response, HttpServletResponse.SC_FORBIDDEN,
                        "認証情報とTenant指定が一致しません。");
                return;
            }
            executeWithTenant(
                    authenticatedTenantId,
                    request,
                    response,
                    filterChain
            );
            return;
        }

        if (isLoginRequest(request)) {
            if (requestedTenantId == null) {
                reject(response, HttpServletResponse.SC_BAD_REQUEST,
                        "ログイン時はX-Tenant-IDが必須です。");
                return;
            }
            executeWithTenant(
                    requestedTenantId,
                    request,
                    response,
                    filterChain
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void executeWithTenant(
            String tenantId,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Session session = entityManager.unwrap(Session.class);
        TenantContext.setTenantId(tenantId);
        session.enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
        session.enableFilter("softDeleteFilter");

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            session.disableFilter("tenantFilter");
            session.disableFilter("softDeleteFilter");
        }
    }

    private String authenticatedTenantId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUser user)) {
            return null;
        }
        return requireValidTenantId(user.getTenantId());
    }

    private String normalizeTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        return requireValidTenantId(tenantId.trim());
    }

    private String requireValidTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)
                || !TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            throw new IllegalArgumentException("Tenant IDの形式が不正です。");
        }
        return tenantId;
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "/auth/login".equals(request.getServletPath());
    }

    private void reject(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
