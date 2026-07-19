package com.project.backend.features.system.batch.service.executor;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.report.service.api.ReportBatchExecutionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportMailBatchJobExecutor implements BatchJobExecutor {

    private final ReportBatchExecutionService reportBatchExecutionService;

    @Override
    public BatchJobType supportType() {
        return BatchJobType.REPORT_MAIL;
    }

    @Override
    public BatchJobExecutionResult execute(BatchJobExecutionContext context) {
        return reportBatchExecutionService.executeWithMail(
            context.targetCode(),
            context.params()
        );
    }
}