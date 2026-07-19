package com.project.backend.features.system.batch.dto;

import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.batch.enums.BatchScheduleType;

public record BatchJobDefinitionSaveRequest(
        String jobCode,
        String jobName,
        BatchJobType jobType,
        String targetCode,
        Boolean immediateExecutable,
        Boolean scheduleEnabled,
        BatchScheduleType scheduleType,
        String cronExpression,
        Boolean activeFlag,
        String description
) {
}