package com.project.backend.features.system.backup.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupRequest;
import com.project.backend.features.system.backup.dto.SingleBackupFile;
import com.project.backend.features.system.backup.service.builder.BackupExecutionResultBuilder;
import com.project.backend.features.system.backup.service.builder.BackupSingleFileBuilder;
import com.project.backend.features.system.backup.service.resolver.BackupOutputResolver;
import com.project.backend.features.system.backup.service.validation.BackupExecutionValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupExecutionService {

    private final BackupSingleFileBuilder singleFileBuilder;
    private final BackupExecutionResultBuilder resultBuilder;
    private final BackupHistoryService historyService;
    private final BackupExecutionValidator validator;
    private final BackupOutputResolver outputResolver;
    private final BackupFileStorageService fileStorageService;

    public BackupExecutionResult execute(List<String> targetCodes) {
        return execute(
                BackupRequest.builder()
                        .targetCodes(targetCodes)
                        .encoding("UTF-8")
                        .zipOutput(null)
                        .build()
        );
    }

    public BackupExecutionResult execute(BackupRequest request) {
        validator.validate(request);
        BackupExecutionResult result = null;

        try {
            List<SingleBackupFile> files = buildFiles(request);

            result = buildResult(
                    request,
                    files
            );

            historyService.saveSuccess(
                    request,
                    result
            );

            return result;

        } catch (Exception e) {
            cleanupStoredFile(result);
            saveFailureHistory(request, e);

            throw e;
        }
    }

    private void cleanupStoredFile(BackupExecutionResult result) {
        if (result == null || result.storedFile() == null) {
            return;
        }
        try {
            fileStorageService.delete(result.storedFile());
        } catch (Exception ignored) {
            // 元の例外を優先する。孤立ファイルはS3運用点検で検知する。
        }
    }

    private void saveFailureHistory(
            BackupRequest request,
            Exception exception
    ) {
        try {
            historyService.saveFailure(request, exception);
        } catch (Exception ignored) {
            // 元のバックアップ例外を優先する。
        }
    }

    private List<SingleBackupFile> buildFiles(BackupRequest request) {
        return request.targetCodes()
                .stream()
                .map(singleFileBuilder::build)
                .toList();
    }

    private BackupExecutionResult buildResult(
            BackupRequest request,
            List<SingleBackupFile> files
    ) {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("バックアップ対象ファイルが生成されませんでした。");
        }

        if (outputResolver.shouldZip(request, files)) {
            return resultBuilder.buildZip(files);
        }

        return resultBuilder.buildSingle(files.get(0));
    }
}
