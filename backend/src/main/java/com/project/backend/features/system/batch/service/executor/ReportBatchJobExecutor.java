package com.project.backend.features.system.batch.service.executor;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.report.service.api.ReportBatchExecutionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportBatchJobExecutor implements BatchJobExecutor {

    private final ReportBatchExecutionService reportBatchExecutionService;

    @Override
    public BatchJobType supportType() {
        return BatchJobType.REPORT;
    }

    @Override
    public BatchJobExecutionResult execute(BatchJobExecutionContext context) {
        return reportBatchExecutionService.execute(
            context.targetCode(),
            context.params()
        );
    }
}