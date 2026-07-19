package com.project.backend.features.system.batch.dto;

import java.time.Instant;

import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.batch.enums.BatchScheduleType;

import lombok.Builder;

@Builder
public record BatchJobDefinitionResponse(
        Long id,
        String jobCode,
        String jobName,
        BatchJobType jobType,
        String targetCode,
        boolean immediateExecutable,
        boolean scheduleEnabled,
        BatchScheduleType scheduleType,
        String cronExpression,
        Instant lastExecutedAt,
        Instant nextExecuteAt,
        boolean activeFlag,
        String description
) {
}