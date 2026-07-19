package com.project.backend.features.system.report.dto;

import java.util.Set;

public record ReportRecipientGenerationSummary(
        int targetCount,
        int successCount,
        int failedCount,
        int skippedCount,
        Set<String> mailTypes
) {
}
