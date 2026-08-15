package com.project.backend.common.filter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);
    private static final String HEALTH_PATH = "/actuator/health";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        String traceId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();

        try {
            MDC.put("traceId", traceId);
            MDC.put("httpMethod", request.getMethod());
            MDC.put("httpPath", request.getRequestURI());
            response.setHeader(TRACE_ID_HEADER, traceId);

            chain.doFilter(request, response);
        } finally {
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startedAt
            );
            MDC.put("httpStatus", Integer.toString(response.getStatus()));
            MDC.put("durationMs", Long.toString(durationMillis));

            if (!HEALTH_PATH.equals(request.getRequestURI())) {
                log.info(
                        "HTTP request completed: method={}, path={}, status={}, durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMillis
                );
            }
            MDC.clear();
        }
    }
}
