package com.project.backend.features.system.batch.service.executor;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.imports.service.ImportExecutionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImportBatchJobExecutor implements BatchJobExecutor {

    private final ImportExecutionService importExecutionService;

    @Override
    public BatchJobType supportType() {
        return BatchJobType.IMPORT;
    }

    @Override
    public BatchJobExecutionResult execute(BatchJobExecutionContext context) {
        String targetCode = context.targetCode();

        importExecutionService.executeFromDefinition(targetCode);

        return BatchJobExecutionResult.message(
                "Importバッチが完了しました。 targetCode=" + targetCode
        );
    }
}