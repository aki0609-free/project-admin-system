package com.project.backend.app.security.jwt.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.security.jwt.services.CustomUserDetailsService;
import com.project.backend.app.security.jwt.services.JwtService;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
        JwtAuthenticationFilter.class
    );

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String username = jwtService.extractUsername(jwt);
            String tenantId = jwtService.extractTenantId(jwt);

            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("JWTにTenant IDがありません。");
            }

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                SecurityUser userDetails =
                    (SecurityUser) userDetailsService.loadUserByUsernameAndTenantId(username, tenantId);

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException exception) {
            rejectAuthentication(response, exception);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rejectAuthentication(
        HttpServletResponse response,
        RuntimeException exception
    ) throws IOException {
        SecurityContextHolder.clearContext();
        log.debug("JWT authentication was rejected: {}", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"message\":\"認証トークンが無効または期限切れです。\"}"
        );
    }

    @SuppressWarnings("null")
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/auth/login") || path.equals("/auth/refresh");
    }
}
