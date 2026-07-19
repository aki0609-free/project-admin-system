package com.project.backend.features.system.batch.dto;

import lombok.Builder;

@Builder
public record BatchJobRunResult(
        Long executionLogId,
        BatchJobExecutionResult result
) {
}