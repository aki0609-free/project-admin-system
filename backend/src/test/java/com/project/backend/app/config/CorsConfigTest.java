package com.project.backend.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    @Test
    void configuredOriginIsAllowed() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://project-admin.fuyo-system.com"));

        CorsConfigurationSource source = new CorsConfig(properties).corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("Origin", "https://project-admin.fuyo-system.com");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly(
                        "http://localhost:5173",
                        "https://project-admin.fuyo-system.com");
    }
}
