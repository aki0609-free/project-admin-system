package com.project.backend.app.openai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.analysis")
public record AiAnalysisProperties(
        String promptVersion
) {
}
