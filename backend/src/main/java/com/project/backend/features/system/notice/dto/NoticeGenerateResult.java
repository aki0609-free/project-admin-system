package com.project.backend.features.system.notice.dto;

import lombok.Builder;

@Builder
public record NoticeGenerateResult(
        int ruleCount,
        int targetCount,
        int generatedCount,
        int skippedCount,
        String message
) {
}