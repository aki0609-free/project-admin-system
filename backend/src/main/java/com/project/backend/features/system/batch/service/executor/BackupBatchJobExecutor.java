package com.project.backend.features.system.batch.service.executor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.backup.service.BackupExecutionService;
import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.enums.BatchJobType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupBatchJobExecutor implements BatchJobExecutor {

    private final BackupExecutionService backupExecutionService;

    @Override
    public BatchJobType supportType() {
        return BatchJobType.BACKUP;
    }

    @Override
    public BatchJobExecutionResult execute(BatchJobExecutionContext context) {
        String targetCode = context.targetCode();

        backupExecutionService.execute(List.of(targetCode));

        return BatchJobExecutionResult.message(
                "Backupバッチが完了しました。 targetCode=" + targetCode
        );
    }
}