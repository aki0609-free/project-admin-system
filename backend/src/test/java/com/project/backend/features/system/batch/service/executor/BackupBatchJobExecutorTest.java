package com.project.backend.features.system.batch.service.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupStoredFile;
import com.project.backend.features.system.backup.service.BackupExecutionService;
import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;

class BackupBatchJobExecutorTest {

    @Test
    void scheduledBackupReturnsStoredFileMetadata() {
        BackupExecutionService service = mock(BackupExecutionService.class);
        BackupStoredFile storedFile = new BackupStoredFile(
                StorageType.S3,
                "documents/backups/system/default/report.csv",
                "report.csv",
                "text/csv",
                100L
        );
        when(service.execute(List.of("BACKUP_REPORT")))
                .thenReturn(BackupExecutionResult.builder()
                        .storedFile(storedFile)
                        .build());

        BatchJobExecutionResult result =
                new BackupBatchJobExecutor(service).execute(
                        context("BACKUP_REPORT")
                );

        assertThat(result.outputFileKey()).isEqualTo(storedFile.fileKey());
        assertThat(result.storageType()).isEqualTo(StorageType.S3);
    }

    @Test
    void downloadOnlyDefinitionIsRejectedForBatchExecution() {
        BackupExecutionService service = mock(BackupExecutionService.class);
        when(service.execute(List.of("BACKUP_REPORT")))
                .thenReturn(BackupExecutionResult.builder().build());

        assertThatThrownBy(() ->
                new BackupBatchJobExecutor(service).execute(
                        context("BACKUP_REPORT")
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SERVER_FILE");
    }

    private BatchJobExecutionContext context(String targetCode) {
        BatchJobDefinition definition = new BatchJobDefinition();
        definition.setTargetCode(targetCode);
        return new BatchJobExecutionContext(
                definition,
                null,
                Map.of()
        );
    }
}
