package com.project.backend.app.openai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiAnalysisProperties.class)
public class AiAnalysisConfig {
}
