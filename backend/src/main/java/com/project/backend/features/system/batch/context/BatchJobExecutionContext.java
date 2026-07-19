package com.project.backend.features.system.batch.context;

import java.util.Map;

import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;

public record BatchJobExecutionContext(
        BatchJobDefinition definition,
        BatchExecutionLog log,
        Map<String, Object> params
) {
    public String jobCode() {
        return definition.getJobCode();
    }

    public String targetCode() {
        return definition.getTargetCode();
    }
}