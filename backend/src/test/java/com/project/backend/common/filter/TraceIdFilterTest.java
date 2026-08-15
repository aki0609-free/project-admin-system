package com.project.backend.common.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void addsTraceIdToResponseAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("traceId")).isNotBlank();
            assertThat(MDC.get("httpMethod")).isEqualTo("GET");
            assertThat(MDC.get("httpPath")).isEqualTo("/api/employees");
            ((HttpServletResponse) servletResponse).setStatus(204);
        });

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER))
                .isNotBlank()
                .matches("[0-9a-f-]{36}");
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
