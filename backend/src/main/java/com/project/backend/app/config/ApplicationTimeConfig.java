package com.project.backend.app.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationTimeConfig {

    public static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Tokyo");

    @Bean
    Clock applicationClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
