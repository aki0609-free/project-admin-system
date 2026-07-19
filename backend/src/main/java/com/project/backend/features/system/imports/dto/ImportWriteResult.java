package com.project.backend.features.system.imports.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ImportWriteResult(
        int totalCount,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        int errorCount,
        List<ImportWriteError> errors
) {
}