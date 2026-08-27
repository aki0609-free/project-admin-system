package com.project.backend.features.system.imports.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.service.resolver.ImportCsvPathResolver;

class ImportExecutionServiceTest {

    @Test
    void executeFromDefinition_shouldRecordFailureBeforeBatchStarts() {
        ImportTargetAdminService targetService = mock(ImportTargetAdminService.class);
        ImportUploadFileService uploadFileService = mock(ImportUploadFileService.class);
        ImportScriptExecutorService scriptService = mock(ImportScriptExecutorService.class);
        ImportCsvJobLauncherService jobLauncher = mock(ImportCsvJobLauncherService.class);
        ImportCsvPathResolver csvPathResolver = mock(ImportCsvPathResolver.class);
        ImportHistoryService historyService = mock(ImportHistoryService.class);
        ImportTargetDefinition target = ImportTargetDefinition.builder()
                .targetCode("IMPORT_TEST")
                .targetName("取込テスト")
                .sourceType(ImportSourceType.SCRIPT)
                .scriptType(ImportScriptType.PYTHON)
                .scriptPath("test.py")
                .fixedFilePath("test.csv")
                .build();

        when(targetService.findByTargetCode("IMPORT_TEST")).thenReturn(target);
        doThrow(new RuntimeException("python converter failed"))
                .when(scriptService)
                .execute(target);

        ImportExecutionService service = new ImportExecutionService(
                targetService,
                uploadFileService,
                scriptService,
                jobLauncher,
                csvPathResolver,
                historyService
        );

        assertThatThrownBy(() -> service.executeFromDefinition("IMPORT_TEST"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("python converter failed");

        verify(historyService).saveFailure(
                eq(target),
                isNull(),
                isNull(),
                eq("system"),
                org.mockito.ArgumentMatchers.argThat(
                        error -> error.getMessage().contains("python converter failed")
                )
        );
    }
}
