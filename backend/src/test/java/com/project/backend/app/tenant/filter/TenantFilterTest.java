package com.project.backend.app.tenant.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.user.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;

class TenantFilterTest {

    private EntityManager entityManager;
    private Session session;
    private Filter tenantHibernateFilter;
    private Filter softDeleteHibernateFilter;
    private FilterChain filterChain;
    private TenantFilter filter;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        session = mock(Session.class);
        tenantHibernateFilter = mock(Filter.class);
        softDeleteHibernateFilter = mock(Filter.class);
        filterChain = mock(FilterChain.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter"))
                .thenReturn(tenantHibernateFilter);
        when(tenantHibernateFilter.setParameter("tenantId", "tenant-a"))
                .thenReturn(tenantHibernateFilter);
        when(tenantHibernateFilter.setParameter("tenantId", "default"))
                .thenReturn(tenantHibernateFilter);
        when(session.enableFilter("softDeleteFilter"))
                .thenReturn(softDeleteHibernateFilter);
        filter = new TenantFilter(entityManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void authenticatedRequest_shouldUsePrincipalTenantWithoutHeader()
            throws Exception {
        authenticate("tenant-a");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/employees"
        );
        request.setServletPath("/api/employees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-a");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tenantHibernateFilter).setParameter("tenantId", "tenant-a");
        verify(session).disableFilter("tenantFilter");
        verify(session).disableFilter("softDeleteFilter");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void authenticatedRequest_shouldRejectHeaderForAnotherTenant()
            throws Exception {
        authenticate("tenant-a");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/employees"
        );
        request.setServletPath("/api/employees");
        request.addHeader("X-Tenant-ID", "tenant-b");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("一致しません");
        verify(filterChain, never()).doFilter(request, response);
        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void login_shouldUseValidatedHeaderTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/auth/login"
        );
        request.setServletPath("/auth/login");
        request.addHeader("X-Tenant-ID", "default");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("default");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tenantHibernateFilter).setParameter("tenantId", "default");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void login_shouldRejectMissingTenantHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/auth/login"
        );
        request.setServletPath("/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("X-Tenant-IDが必須");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void unauthenticatedProtectedRequest_shouldNotTrustTenantHeader()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/employees"
        );
        request.setServletPath("/api/employees");
        request.addHeader("X-Tenant-ID", "tenant-a");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(entityManager, never()).unwrap(Session.class);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    private void authenticate(String tenantId) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername("admin");
        user.setPassword("unused");
        user.setEnabled(true);
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
