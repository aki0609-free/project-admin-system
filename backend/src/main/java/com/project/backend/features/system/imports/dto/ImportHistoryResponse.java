package com.project.backend.features.system.imports.dto;

import java.time.Instant;

import com.project.backend.features.system.imports.enums.ImportHistoryStatus;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportSourceType;

import lombok.Builder;

@Builder
public record ImportHistoryResponse(
        Long id,

        String targetCode,
        String targetName,
        String tableName,

        ImportSourceType sourceType,
        ImportMode importMode,

        String fileName,

        int totalCount,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        int errorCount,

        ImportHistoryStatus status,

        Long jobExecutionId,

        String executedBy,
        Instant executedAt,

        String errorMessage
) {
}