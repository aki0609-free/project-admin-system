package com.project.backend.features.system.batch.service.executor;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.enums.BatchJobType;

public interface BatchJobExecutor {

    BatchJobType supportType();

    BatchJobExecutionResult execute(BatchJobExecutionContext context);
}